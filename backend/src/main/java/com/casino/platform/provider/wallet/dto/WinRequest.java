package com.casino.platform.provider.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Request DTO for win (credit) operation.
 */
public class WinRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Game ID is required")
    private String gameId;

    @NotBlank(message = "Round ID is required")
    private String roundId;

    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must be zero or positive")
    private BigDecimal amount;

    @NotBlank(message = "Original transaction ID is required")
    private String originalTransactionId;

    // Constructors
    public WinRequest() {
    }

    public WinRequest(String transactionId, String playerId, String currency,
                      String gameId, String roundId, BigDecimal amount,
                      String originalTransactionId) {
        this.transactionId = transactionId;
        this.playerId = playerId;
        this.currency = currency;
        this.gameId = gameId;
        this.roundId = roundId;
        this.amount = amount;
        this.originalTransactionId = originalTransactionId;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getRoundId() {
        return roundId;
    }

    public void setRoundId(String roundId) {
        this.roundId = roundId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }
}
