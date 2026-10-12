package com.casino.platform.game.repository;

import com.casino.platform.game.entity.GameRound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, Long> {

    Page<GameRound> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<GameRound> findByGameIdOrderByCreatedAtDesc(Long gameId, Pageable pageable);
}
