package com.casino.platform.provider.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for bet (debit) operation.
 */
public class BetRequest {

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
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    // Constructors
    public BetRequest() {
    }

    public BetRequest(String transactionId, String playerId, String currency,
                      String gameId, String roundId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.playerId = playerId;
        this.currency = currency;
        this.gameId = gameId;
        this.roundId = roundId;
        this.amount = amount;
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
}
