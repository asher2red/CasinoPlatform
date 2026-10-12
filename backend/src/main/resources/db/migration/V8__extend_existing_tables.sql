-- V8: Extend existing tables for seamless wallet integration

-- ============================================================================
-- 1. Add provider_transaction_id to ledger table
-- ============================================================================
-- Links ledger entries to provider transactions for audit trail
ALTER TABLE ledger
ADD COLUMN provider_transaction_id BIGINT;

-- Add foreign key constraint
ALTER TABLE ledger
ADD CONSTRAINT fk_ledger_provider_transaction
    FOREIGN KEY (provider_transaction_id)
    REFERENCES provider_transactions(id)
    ON DELETE SET NULL;

-- Add index for faster lookups
CREATE INDEX idx_ledger_provider_tx_id ON ledger(provider_transaction_id);

COMMENT ON COLUMN ledger.provider_transaction_id IS 'Link to provider transaction (for provider wallet API operations)';

-- ============================================================================
-- 2. Add provider fields to games table
-- ============================================================================
-- Enables games to be associated with specific providers
ALTER TABLE games
ADD COLUMN provider_id BIGINT,
ADD COLUMN provider_game_id VARCHAR(100);

-- Add foreign key constraint
ALTER TABLE games
ADD CONSTRAINT fk_games_provider
    FOREIGN KEY (provider_id)
    REFERENCES providers(id)
    ON DELETE SET NULL;

-- Add index
CREATE INDEX idx_games_provider_id ON games(provider_id);
CREATE INDEX idx_games_provider_game_id ON games(provider_game_id);

COMMENT ON COLUMN games.provider_id IS 'External provider for this game (NULL for platform-native games)';
COMMENT ON COLUMN games.provider_game_id IS 'Provider-specific game identifier';

-- ============================================================================
-- 3. Migrate existing data
-- ============================================================================
-- Set all existing games to use DEMO provider
UPDATE games
SET provider_id = (SELECT id FROM providers WHERE code = 'DEMO')
WHERE provider_id IS NULL;

-- Set provider_game_id to game ID for existing games
UPDATE games
SET provider_game_id = CAST(id AS VARCHAR)
WHERE provider_game_id IS NULL AND provider_id IS NOT NULL;

-- ============================================================================
-- 4. Verification queries (commented out, for manual checking)
-- ============================================================================
-- Check ledger updates
-- SELECT COUNT(*) FROM ledger WHERE provider_transaction_id IS NOT NULL;

-- Check games migration
-- SELECT id, name, provider_id, provider_game_id
-- FROM games
-- ORDER BY id;

-- Verify provider linkage
-- SELECT g.id, g.name, p.code AS provider_code, g.provider_game_id
-- FROM games g
-- LEFT JOIN providers p ON g.provider_id = p.id
-- ORDER BY g.id;
