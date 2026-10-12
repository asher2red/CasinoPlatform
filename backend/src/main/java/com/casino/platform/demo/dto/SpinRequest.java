package com.casino.platform.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for demo game spin.
 */
public class SpinRequest {

    @NotNull(message = "Bet per line is required")
    @DecimalMin(value = "0.01", message = "Bet per line must be at least 0.01")
    private BigDecimal betPerLine;

    // Constructors
    public SpinRequest() {
    }

    public SpinRequest(BigDecimal betPerLine) {
        this.betPerLine = betPerLine;
    }

    // Getters and Setters
    public BigDecimal getBetPerLine() {
        return betPerLine;
    }

    public void setBetPerLine(BigDecimal betPerLine) {
        this.betPerLine = betPerLine;
    }
}
