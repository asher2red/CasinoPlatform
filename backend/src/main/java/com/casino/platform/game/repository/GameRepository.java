package com.casino.platform.game.repository;

import com.casino.platform.game.entity.Game;
import com.casino.platform.game.entity.GameCategory;
import com.casino.platform.game.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByStatus(GameStatus status);

    List<Game> findByCategory(GameCategory category);

    List<Game> findByCategoryAndStatus(GameCategory category, GameStatus status);
}
