-- Create wallets table
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    currency_type VARCHAR(20) NOT NULL DEFAULT 'DEMO',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallets_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_currency_type CHECK (currency_type IN ('DEMO'))
);

-- Create index on user_id
CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Create ledger table for transaction history
CREATE TABLE ledger (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    balance_before NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    description TEXT,
    reference_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_wallet_id FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    CONSTRAINT check_transaction_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAW', 'GAME_BET', 'GAME_WIN', 'ADJUSTMENT'))
);

-- Create indexes for ledger
CREATE INDEX idx_ledger_wallet_id ON ledger(wallet_id);
CREATE INDEX idx_ledger_reference_id ON ledger(reference_id);
CREATE INDEX idx_ledger_created_at ON ledger(created_at DESC);

-- Add comments
COMMENT ON TABLE wallets IS 'User virtual currency wallets';
COMMENT ON COLUMN wallets.balance IS 'Current wallet balance, must be non-negative';
COMMENT ON COLUMN wallets.version IS 'Optimistic locking version for concurrency control';

COMMENT ON TABLE ledger IS 'Transaction history for all wallet operations';
COMMENT ON COLUMN ledger.reference_id IS 'External reference ID for idempotency';
COMMENT ON COLUMN ledger.balance_before IS 'Wallet balance before this transaction';
COMMENT ON COLUMN ledger.balance_after IS 'Wallet balance after this transaction';
