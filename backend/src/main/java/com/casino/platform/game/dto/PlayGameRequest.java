package com.casino.platform.game.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlayGameRequest {

    @NotNull(message = "Bet per line is required")
    @DecimalMin(value = "0.01", message = "Bet per line must be at least 0.01")
    private BigDecimal betPerLine;

    public PlayGameRequest() {}

    public PlayGameRequest(BigDecimal betPerLine) {
        this.betPerLine = betPerLine;
    }

    public BigDecimal getBetPerLine() {
        return betPerLine;
    }

    public void setBetPerLine(BigDecimal betPerLine) {
        this.betPerLine = betPerLine;
    }
}
