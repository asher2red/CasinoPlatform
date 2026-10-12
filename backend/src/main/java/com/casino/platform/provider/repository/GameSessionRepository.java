package com.casino.platform.provider.repository;

import com.casino.platform.provider.entity.GameSession;
import com.casino.platform.provider.entity.GameSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    Optional<GameSession> findBySessionTokenHash(String sessionTokenHash);

    List<GameSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    List<GameSession> findByUserId(Long userId);

    @Query("SELECT gs FROM GameSession gs WHERE gs.status = :status AND gs.expiresAt < :now")
    List<GameSession> findExpiredSessions(@Param("status") SessionStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT gs FROM GameSession gs WHERE gs.user.id = :userId AND gs.status = 'ACTIVE' AND gs.expiresAt > :now")
    List<GameSession> findActiveSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT gs FROM GameSession gs WHERE gs.sessionTokenHash = :tokenHash AND gs.status = 'ACTIVE' AND gs.expiresAt > :now")
    Optional<GameSession> findActiveSessionByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
}
