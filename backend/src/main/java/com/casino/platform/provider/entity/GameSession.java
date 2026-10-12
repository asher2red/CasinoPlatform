package com.casino.platform.provider.entity;

import com.casino.platform.game.entity.Game;
import com.casino.platform.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Active game session with authentication token.
 * Links user, provider, and game for a specific play session.
 */
@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "session_token_hash", nullable = false, unique = true, length = 64)
    private String sessionTokenHash;

    @Column(nullable = false, length = 20)
    private String currency = "DEMO";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            // Default expiration: 24 hours
            expiresAt = createdAt.plusHours(24);
        }
    }

    // Constructors
    public GameSession() {
    }

    public GameSession(User user, Provider provider, Game game, String sessionTokenHash) {
        this.user = user;
        this.provider = provider;
        this.game = game;
        this.sessionTokenHash = sessionTokenHash;
    }

    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE && !isExpired();
    }

    public void close() {
        this.status = SessionStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = SessionStatus.EXPIRED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getSessionTokenHash() {
        return sessionTokenHash;
    }

    public void setSessionTokenHash(String sessionTokenHash) {
        this.sessionTokenHash = sessionTokenHash;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public enum SessionStatus {
        ACTIVE,
        EXPIRED,
        CLOSED
    }
}
