package com.casino.platform.provider.wallet.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rollback operation.
 */
public class RollbackRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Original transaction ID is required")
    private String originalTransactionId;

    // Constructors
    public RollbackRequest() {
    }

    public RollbackRequest(String transactionId, String playerId,
                           String currency, String originalTransactionId) {
        this.transactionId = transactionId;
        this.playerId = playerId;
        this.currency = currency;
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

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }
}
