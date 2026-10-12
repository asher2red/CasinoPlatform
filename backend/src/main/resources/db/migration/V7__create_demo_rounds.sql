-- V7: Create demo rounds table for demo provider game state

-- ============================================================================
-- Demo rounds: Tracks demo slot game rounds with bet, result, and settlement
-- ============================================================================
CREATE TABLE demo_rounds (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    game_session_id BIGINT NOT NULL,
    round_id VARCHAR(200) NOT NULL UNIQUE,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    bet_amount NUMERIC(19, 2) NOT NULL CHECK (bet_amount > 0),
    bet_transaction_id VARCHAR(200),
    win_amount NUMERIC(19, 2) CHECK (win_amount >= 0),
    win_transaction_id VARCHAR(200),
    reel_result JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'BET_PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_demo_round_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_demo_round_session FOREIGN KEY (game_session_id)
        REFERENCES game_sessions(id) ON DELETE CASCADE,

    CONSTRAINT check_demo_round_status CHECK (
        status IN (
            'BET_PENDING',     -- Bet request initiated
            'BET_SUCCESS',     -- Bet deducted, result calculated
            'WIN_PENDING',     -- Win credit initiated
            'WIN_SUCCESS',     -- Win credited, round complete
            'FAILED'           -- Round failed (for recovery)
        )
    )
);

CREATE INDEX idx_demo_rounds_user_id ON demo_rounds(user_id);
CREATE INDEX idx_demo_rounds_session_id ON demo_rounds(game_session_id);
CREATE INDEX idx_demo_rounds_round_id ON demo_rounds(round_id);
CREATE INDEX idx_demo_rounds_idempotency_key ON demo_rounds(idempotency_key);
CREATE INDEX idx_demo_rounds_status ON demo_rounds(status);
CREATE INDEX idx_demo_rounds_created_at ON demo_rounds(created_at DESC);
CREATE INDEX idx_demo_rounds_bet_tx_id ON demo_rounds(bet_transaction_id);
CREATE INDEX idx_demo_rounds_win_tx_id ON demo_rounds(win_transaction_id);

COMMENT ON TABLE demo_rounds IS 'Demo slot game rounds with state management for idempotency';
COMMENT ON COLUMN demo_rounds.round_id IS 'Unique round identifier (server-generated UUID)';
COMMENT ON COLUMN demo_rounds.idempotency_key IS 'Client-provided idempotency key from request header';
COMMENT ON COLUMN demo_rounds.bet_amount IS 'Total bet amount (betPerLine * 25)';
COMMENT ON COLUMN demo_rounds.bet_transaction_id IS 'Provider transaction ID for bet';
COMMENT ON COLUMN demo_rounds.win_amount IS 'Win amount after RTP adjustment (NULL if not yet calculated)';
COMMENT ON COLUMN demo_rounds.win_transaction_id IS 'Provider transaction ID for win';
COMMENT ON COLUMN demo_rounds.reel_result IS 'JSON: {reels: [[]], winningLines: [...]}';
COMMENT ON COLUMN demo_rounds.status IS 'Round state for idempotency and recovery';

-- ============================================================================
-- State transition diagram:
-- ============================================================================
-- BET_PENDING → BET_SUCCESS → WIN_PENDING → WIN_SUCCESS (normal flow)
--     ↓              ↓             ↓             ↓
--   FAILED       FAILED        FAILED     (completed)
--
-- On retry with same idempotency_key:
-- - BET_PENDING: retry bet
-- - BET_SUCCESS: return saved result (no re-spin)
-- - WIN_PENDING: retry win credit
-- - WIN_SUCCESS: return saved result
-- - FAILED: requires manual investigation
-- ============================================================================

-- ============================================================================
-- Verification queries (commented out, for manual testing)
-- ============================================================================
-- Show recent rounds
-- SELECT id, user_id, round_id, bet_amount, win_amount, status, created_at
-- FROM demo_rounds
-- ORDER BY created_at DESC
-- LIMIT 10;

-- Find incomplete rounds (for recovery)
-- SELECT id, user_id, round_id, status, created_at
-- FROM demo_rounds
-- WHERE status IN ('BET_PENDING', 'BET_SUCCESS', 'WIN_PENDING')
-- ORDER BY created_at;

-- Check specific round by idempotency key
-- SELECT * FROM demo_rounds WHERE idempotency_key = 'your-key-here';
