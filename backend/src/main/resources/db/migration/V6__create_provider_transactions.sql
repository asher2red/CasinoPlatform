-- V6: Create provider transactions, nonce store, and rollback intents

-- ============================================================================
-- Provider transactions: Records all provider wallet API requests and responses
-- ============================================================================
CREATE TABLE provider_transactions (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    transaction_id VARCHAR(200) NOT NULL,
    user_id BIGINT NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2),
    currency VARCHAR(20) NOT NULL,
    game_id BIGINT,
    round_id VARCHAR(200),
    original_transaction_id VARCHAR(200),
    ledger_id BIGINT,
    balance_after NUMERIC(19, 2),
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    request_body TEXT,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_provider_tx_provider FOREIGN KEY (provider_id)
        REFERENCES providers(id) ON DELETE CASCADE,
    CONSTRAINT fk_provider_tx_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_provider_tx_game FOREIGN KEY (game_id)
        REFERENCES games(id) ON DELETE SET NULL,
    CONSTRAINT fk_provider_tx_ledger FOREIGN KEY (ledger_id)
        REFERENCES ledger(id) ON DELETE SET NULL,

    -- Each (provider, transaction_id) must be unique
    CONSTRAINT uq_provider_transaction UNIQUE (provider_id, transaction_id),

    CONSTRAINT check_operation_type CHECK (
        operation_type IN ('BALANCE', 'BET', 'WIN', 'ROLLBACK')
    ),
    CONSTRAINT check_tx_status CHECK (
        status IN ('SUCCESS', 'FAILED', 'PENDING')
    ),
    CONSTRAINT check_currency CHECK (
        currency IN ('DEMO')
    )
);

CREATE INDEX idx_provider_tx_provider_id ON provider_transactions(provider_id);
CREATE INDEX idx_provider_tx_user_id ON provider_transactions(user_id);
CREATE INDEX idx_provider_tx_transaction_id ON provider_transactions(transaction_id);
CREATE INDEX idx_provider_tx_operation_type ON provider_transactions(operation_type);
CREATE INDEX idx_provider_tx_status ON provider_transactions(status);
CREATE INDEX idx_provider_tx_round_id ON provider_transactions(round_id);
CREATE INDEX idx_provider_tx_original_tx_id ON provider_transactions(original_transaction_id);
CREATE INDEX idx_provider_tx_created_at ON provider_transactions(created_at DESC);

COMMENT ON TABLE provider_transactions IS 'Records all provider wallet API operations';
COMMENT ON COLUMN provider_transactions.transaction_id IS 'Provider-provided transaction ID (idempotency key)';
COMMENT ON COLUMN provider_transactions.operation_type IS 'BALANCE: balance check, BET: debit, WIN: credit, ROLLBACK: cancel';
COMMENT ON COLUMN provider_transactions.amount IS 'Transaction amount (NULL for BALANCE operations)';
COMMENT ON COLUMN provider_transactions.round_id IS 'Game round identifier';
COMMENT ON COLUMN provider_transactions.original_transaction_id IS 'Reference to original transaction for WIN/ROLLBACK';
COMMENT ON COLUMN provider_transactions.ledger_id IS 'Link to ledger entry if balance changed';
COMMENT ON COLUMN provider_transactions.balance_after IS 'Wallet balance after transaction';
COMMENT ON COLUMN provider_transactions.status IS 'SUCCESS: completed, FAILED: rejected, PENDING: in progress';
COMMENT ON COLUMN provider_transactions.error_code IS 'Error code if status = FAILED';
COMMENT ON COLUMN provider_transactions.request_body IS 'Original request JSON for audit';
COMMENT ON COLUMN provider_transactions.response_body IS 'Response JSON sent to provider';

-- ============================================================================
-- Nonce store: Prevents nonce reuse in HMAC authentication
-- ============================================================================
CREATE TABLE nonce_store (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    nonce VARCHAR(100) NOT NULL,
    timestamp_ms BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_nonce_provider FOREIGN KEY (provider_id)
        REFERENCES providers(id) ON DELETE CASCADE,

    -- Each (provider, nonce) must be unique
    CONSTRAINT uq_provider_nonce UNIQUE (provider_id, nonce)
);

CREATE INDEX idx_nonce_store_expires_at ON nonce_store(expires_at);
CREATE INDEX idx_nonce_store_provider_id ON nonce_store(provider_id);
CREATE INDEX idx_nonce_store_timestamp ON nonce_store(timestamp_ms);

COMMENT ON TABLE nonce_store IS 'Stores used nonces to prevent replay attacks';
COMMENT ON COLUMN nonce_store.nonce IS 'Unique nonce value from provider request';
COMMENT ON COLUMN nonce_store.timestamp_ms IS 'Request timestamp in milliseconds';
COMMENT ON COLUMN nonce_store.expires_at IS 'When this nonce record can be deleted';

-- ============================================================================
-- Rollback intents: Handles rollback arriving before original transaction
-- ============================================================================
CREATE TABLE rollback_intents (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    rollback_transaction_id VARCHAR(200) NOT NULL,
    original_transaction_id VARCHAR(200) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rollback_intent_provider FOREIGN KEY (provider_id)
        REFERENCES providers(id) ON DELETE CASCADE,
    CONSTRAINT fk_rollback_intent_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- Each (provider, rollback_transaction_id) must be unique
    CONSTRAINT uq_rollback_intent UNIQUE (provider_id, rollback_transaction_id)
);

CREATE INDEX idx_rollback_intent_provider_id ON rollback_intents(provider_id);
CREATE INDEX idx_rollback_intent_original_tx_id ON rollback_intents(original_transaction_id);
CREATE INDEX idx_rollback_intent_user_id ON rollback_intents(user_id);
CREATE INDEX idx_rollback_intent_created_at ON rollback_intents(created_at);

COMMENT ON TABLE rollback_intents IS 'Records rollback requests that arrived before the original transaction';
COMMENT ON COLUMN rollback_intents.rollback_transaction_id IS 'Transaction ID of the rollback request';
COMMENT ON COLUMN rollback_intents.original_transaction_id IS 'Transaction ID to be rolled back';
COMMENT ON COLUMN rollback_intents.user_id IS 'User associated with the rollback';

-- ============================================================================
-- Verification queries (commented out, for manual testing)
-- ============================================================================
-- SELECT * FROM provider_transactions WHERE status = 'SUCCESS' ORDER BY created_at DESC LIMIT 10;
-- SELECT * FROM nonce_store WHERE expires_at > CURRENT_TIMESTAMP ORDER BY created_at DESC;
-- SELECT * FROM rollback_intents ORDER BY created_at DESC;

-- Cleanup old nonces (can be run periodically)
-- DELETE FROM nonce_store WHERE expires_at < CURRENT_TIMESTAMP;
