package com.casino.platform.demo.repository;

import com.casino.platform.demo.entity.DemoRound;
import com.casino.platform.demo.entity.DemoRound.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemoRoundRepository extends JpaRepository<DemoRound, Long> {

    /**
     * Find round by round ID.
     */
    Optional<DemoRound> findByRoundId(String roundId);

    /**
     * Find round by idempotency key (for idempotent requests).
     */
    Optional<DemoRound> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find rounds by user ID.
     */
    List<DemoRound> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find rounds by session ID.
     */
    List<DemoRound> findByGameSessionIdOrderByCreatedAtDesc(Long sessionId);

    /**
     * Find incomplete rounds (for recovery/cleanup).
     */
    List<DemoRound> findByStatusInOrderByCreatedAtAsc(List<RoundStatus> statuses);

    /**
     * Check if idempotency key exists.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
