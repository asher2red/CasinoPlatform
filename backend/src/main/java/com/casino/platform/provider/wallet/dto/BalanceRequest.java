package com.casino.platform.provider.wallet.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for balance inquiry.
 */
public class BalanceRequest {

    @NotBlank(message = "Player ID is required")
    private String playerId;

    // Constructors
    public BalanceRequest() {
    }

    public BalanceRequest(String playerId) {
        this.playerId = playerId;
    }

    // Getters and Setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
}
