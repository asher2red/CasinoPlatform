package com.casino.platform.game.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for launching a game.
 */
public class LaunchGameRequest {

    @NotBlank(message = "Currency is required")
    private String currency = "DEMO";

    // Constructors
    public LaunchGameRequest() {
    }

    public LaunchGameRequest(String currency) {
        this.currency = currency;
    }

    // Getters and Setters
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
