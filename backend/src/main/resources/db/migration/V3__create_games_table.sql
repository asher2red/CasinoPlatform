-- Create games table
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(20) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    min_bet NUMERIC(19, 2),
    max_bet NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_game_category CHECK (category IN ('SLOTS', 'CARDS', 'ROULETTE', 'DICE', 'OTHER')),
    CONSTRAINT check_game_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);

-- Create indexes
CREATE INDEX idx_games_category ON games(category);
CREATE INDEX idx_games_status ON games(status);

-- Add comments
COMMENT ON TABLE games IS 'Available casino games';
COMMENT ON COLUMN games.status IS 'Game status: ACTIVE, INACTIVE, or MAINTENANCE';
COMMENT ON COLUMN games.min_bet IS 'Minimum bet amount';
COMMENT ON COLUMN games.max_bet IS 'Maximum bet amount';

-- Insert sample games
INSERT INTO games (name, category, description, status, min_bet, max_bet) VALUES
('Lucky Slots', 'SLOTS', 'Classic slot machine with 3 reels and lucky symbols', 'ACTIVE', 1.00, 100.00),
('Diamond Slots', 'SLOTS', 'Premium slot game with diamond theme and bonus rounds', 'ACTIVE', 5.00, 500.00),
('Blackjack Classic', 'CARDS', 'Traditional blackjack with standard rules', 'ACTIVE', 10.00, 1000.00),
('Poker Texas Hold''em', 'CARDS', 'Texas Hold''em poker against the house', 'ACTIVE', 10.00, 1000.00),
('European Roulette', 'ROULETTE', 'Classic European roulette with single zero', 'ACTIVE', 1.00, 500.00),
('Dice Roll', 'DICE', 'Simple dice game - predict the outcome', 'ACTIVE', 1.00, 100.00),
('Mega Slots', 'SLOTS', 'High stakes slot machine with progressive jackpot', 'INACTIVE', 10.00, 5000.00);
