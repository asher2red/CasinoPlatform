package com.casino.platform.game.entity;

import com.casino.platform.game.entity.Game;
import com.casino.platform.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "game_rounds")
public class GameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "bet_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal betAmount;

    @Column(name = "win_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal winAmount = BigDecimal.ZERO;

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "game_data", columnDefinition = "jsonb")
    private Map<String, Object> gameData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public GameRound() {}

    public GameRound(User user, Game game, BigDecimal betAmount, BigDecimal winAmount,
                     BigDecimal balanceBefore, BigDecimal balanceAfter, Map<String, Object> gameData) {
        this.user = user;
        this.game = game;
        this.betAmount = betAmount;
        this.winAmount = winAmount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.gameData = gameData;
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

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = betAmount;
    }

    public BigDecimal getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(BigDecimal winAmount) {
        this.winAmount = winAmount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Map<String, Object> getGameData() {
        return gameData;
    }

    public void setGameData(Map<String, Object> gameData) {
        this.gameData = gameData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
