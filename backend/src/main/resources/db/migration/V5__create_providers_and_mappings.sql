-- V5: Create providers, player mappings, and game sessions

-- ============================================================================
-- Providers table: External game providers
-- ============================================================================
CREATE TABLE providers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    key_id VARCHAR(100) NOT NULL,
    secret_key VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_providers_code ON providers(code);
CREATE INDEX idx_providers_is_active ON providers(is_active);

COMMENT ON TABLE providers IS 'External game providers configuration';
COMMENT ON COLUMN providers.code IS 'Unique provider identifier (e.g., DEMO, EVOLUTION)';
COMMENT ON COLUMN providers.key_id IS 'Provider API key ID for authentication';
COMMENT ON COLUMN providers.secret_key IS 'HMAC secret key (encrypted in production)';
COMMENT ON COLUMN providers.is_active IS 'Whether provider is currently active';

-- ============================================================================
-- Player-Provider mappings: Links platform users to provider player IDs
-- ============================================================================
CREATE TABLE player_provider_mappings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    provider_player_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_player_provider_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_player_provider_provider FOREIGN KEY (provider_id)
        REFERENCES providers(id) ON DELETE CASCADE,

    -- Each user can have only one player ID per provider
    CONSTRAINT uq_user_provider UNIQUE (user_id, provider_id),
    -- Each provider player ID is unique within that provider
    CONSTRAINT uq_provider_player UNIQUE (provider_id, provider_player_id)
);

CREATE INDEX idx_player_provider_user_id ON player_provider_mappings(user_id);
CREATE INDEX idx_player_provider_provider_id ON player_provider_mappings(provider_id);
CREATE INDEX idx_player_provider_player_id ON player_provider_mappings(provider_player_id);

COMMENT ON TABLE player_provider_mappings IS 'Maps platform users to provider-specific player IDs';
COMMENT ON COLUMN player_provider_mappings.provider_player_id IS 'Player ID in provider system';

-- ============================================================================
-- Game sessions: Tracks active game sessions with session tokens
-- ============================================================================
CREATE TABLE game_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    session_token_hash VARCHAR(64) NOT NULL UNIQUE,
    currency VARCHAR(20) NOT NULL DEFAULT 'DEMO',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,

    CONSTRAINT fk_game_session_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_game_session_provider FOREIGN KEY (provider_id)
        REFERENCES providers(id) ON DELETE CASCADE,
    CONSTRAINT fk_game_session_game FOREIGN KEY (game_id)
        REFERENCES games(id) ON DELETE CASCADE,

    CONSTRAINT check_session_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CLOSED')),
    CONSTRAINT check_session_currency CHECK (currency IN ('DEMO'))
);

CREATE INDEX idx_game_sessions_user_id ON game_sessions(user_id);
CREATE INDEX idx_game_sessions_provider_id ON game_sessions(provider_id);
CREATE INDEX idx_game_sessions_status ON game_sessions(status);
CREATE INDEX idx_game_sessions_expires_at ON game_sessions(expires_at);
CREATE INDEX idx_game_sessions_token_hash ON game_sessions(session_token_hash);

COMMENT ON TABLE game_sessions IS 'Active game sessions with authentication tokens';
COMMENT ON COLUMN game_sessions.session_token_hash IS 'SHA-256 hash of session token';
COMMENT ON COLUMN game_sessions.status IS 'ACTIVE: in use, EXPIRED: timed out, CLOSED: manually closed';
COMMENT ON COLUMN game_sessions.expires_at IS 'Session expiration time (default 24 hours)';

-- ============================================================================
-- Initial data: DEMO provider
-- ============================================================================
INSERT INTO providers (code, name, key_id, secret_key, is_active) VALUES
(
    'DEMO',
    'Demo Game Provider',
    'demo-key-001',
    'demo-secret-key-change-in-production-use-env-variable',
    true
);

-- ============================================================================
-- Verification queries (commented out, for manual testing)
-- ============================================================================
-- SELECT * FROM providers;
-- SELECT * FROM player_provider_mappings;
-- SELECT * FROM game_sessions WHERE status = 'ACTIVE';
