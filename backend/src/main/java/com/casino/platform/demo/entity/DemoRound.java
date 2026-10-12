package com.casino.platform.demo.entity;

import com.casino.platform.provider.entity.GameSession;
import com.casino.platform.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Demo slot game round entity.
 * Tracks the state of a demo game round with bet and win transactions.
 */
@Entity
@Table(name = "demo_rounds")
public class DemoRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @Column(name = "round_id", nullable = false, unique = true, length = 200)
    private String roundId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "bet_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal betAmount;

    @Column(name = "bet_transaction_id", length = 200)
    private String betTransactionId;

    @Column(name = "win_amount", precision = 19, scale = 2)
    private BigDecimal winAmount;

    @Column(name = "win_transaction_id", length = 200)
    private String winTransactionId;

    @Column(name = "reel_result", columnDefinition = "jsonb")
    private String reelResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoundStatus status = RoundStatus.BET_PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public DemoRound() {
    }

    public DemoRound(User user, GameSession gameSession, String roundId,
                     String idempotencyKey, BigDecimal betAmount) {
        this.user = user;
        this.gameSession = gameSession;
        this.roundId = roundId;
        this.idempotencyKey = idempotencyKey;
        this.betAmount = betAmount;
    }

    // Business methods
    public void markBetSuccess(String betTransactionId) {
        this.betTransactionId = betTransactionId;
        this.status = RoundStatus.BET_SUCCESS;
    }

    public void markWinPending() {
        this.status = RoundStatus.WIN_PENDING;
    }

    public void markWinSuccess(String winTransactionId, BigDecimal winAmount) {
        this.winTransactionId = winTransactionId;
        this.winAmount = winAmount;
        this.status = RoundStatus.WIN_SUCCESS;
    }

    public void markFailed() {
        this.status = RoundStatus.FAILED;
    }

    public boolean isCompleted() {
        return status == RoundStatus.WIN_SUCCESS;
    }

    public boolean isFailed() {
        return status == RoundStatus.FAILED;
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

    public GameSession getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public String getRoundId() {
        return roundId;
    }

    public void setRoundId(String roundId) {
        this.roundId = roundId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = betAmount;
    }

    public String getBetTransactionId() {
        return betTransactionId;
    }

    public void setBetTransactionId(String betTransactionId) {
        this.betTransactionId = betTransactionId;
    }

    public BigDecimal getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(BigDecimal winAmount) {
        this.winAmount = winAmount;
    }

    public String getWinTransactionId() {
        return winTransactionId;
    }

    public void setWinTransactionId(String winTransactionId) {
        this.winTransactionId = winTransactionId;
    }

    public String getReelResult() {
        return reelResult;
    }

    public void setReelResult(String reelResult) {
        this.reelResult = reelResult;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Enum
    public enum RoundStatus {
        BET_PENDING,    // Bet request initiated
        BET_SUCCESS,    // Bet deducted, result calculated
        WIN_PENDING,    // Win credit initiated
        WIN_SUCCESS,    // Win credited, round complete
        FAILED          // Round failed (for recovery)
    }
}
