-- Game rounds table for recording game play history
CREATE TABLE game_rounds (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    game_id BIGINT NOT NULL REFERENCES games(id),
    bet_amount NUMERIC(19, 2) NOT NULL CHECK (bet_amount > 0),
    win_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (win_amount >= 0),
    balance_before NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    game_data JSONB, -- Store game-specific data (e.g., reel results, cards dealt)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_game_rounds_user_id ON game_rounds(user_id);
CREATE INDEX idx_game_rounds_game_id ON game_rounds(game_id);
CREATE INDEX idx_game_rounds_created_at ON game_rounds(created_at DESC);
