package com.casino.platform.game.service;

import com.casino.platform.game.entity.Game;
import com.casino.platform.game.entity.GameCategory;
import com.casino.platform.game.entity.GameStatus;
import com.casino.platform.game.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public List<Game> getAllActiveGames() {
        return gameRepository.findByStatus(GameStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesByCategory(GameCategory category) {
        return gameRepository.findByCategoryAndStatus(category, GameStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
    }
}
