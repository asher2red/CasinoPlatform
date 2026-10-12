package com.casino.platform.demo.dto;

import com.casino.platform.game.service.SlotGameEngine.SpinResult;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Response DTO for demo game spin.
 */
public class SpinResponse {

    private String[][] reels;
    private BigDecimal winAmount;
    private BigDecimal balance;

    @JsonProperty("winningLines")
    private java.util.List<WinningLineDto> winningLines;

    private String roundId;
    private String status;

    // Constructors
    public SpinResponse() {
    }

    public SpinResponse(String[][] reels, BigDecimal winAmount, BigDecimal balance,
                        java.util.List<WinningLineDto> winningLines, String roundId, String status) {
        this.reels = reels;
        this.winAmount = winAmount;
        this.balance = balance;
        this.winningLines = winningLines;
        this.roundId = roundId;
        this.status = status;
    }

    // Static factory method
    public static SpinResponse from(SpinResult spinResult, BigDecimal balance, String roundId, String status) {
        java.util.List<WinningLineDto> winningLines = spinResult.getWinningLines().stream()
                .map(WinningLineDto::from)
                .collect(java.util.stream.Collectors.toList());

        return new SpinResponse(
                spinResult.getReels(),
                spinResult.getTotalWin(),
                balance,
                winningLines,
                roundId,
                status
        );
    }

    // Getters and Setters
    public String[][] getReels() {
        return reels;
    }

    public void setReels(String[][] reels) {
        this.reels = reels;
    }

    public BigDecimal getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(BigDecimal winAmount) {
        this.winAmount = winAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public java.util.List<WinningLineDto> getWinningLines() {
        return winningLines;
    }

    public void setWinningLines(java.util.List<WinningLineDto> winningLines) {
        this.winningLines = winningLines;
    }

    public String getRoundId() {
        return roundId;
    }

    public void setRoundId(String roundId) {
        this.roundId = roundId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Inner DTO for winning line
    public static class WinningLineDto {
        private int lineNumber;
        private String symbol;
        private int count;
        private BigDecimal win;

        public WinningLineDto() {
        }

        public WinningLineDto(int lineNumber, String symbol, int count, BigDecimal win) {
            this.lineNumber = lineNumber;
            this.symbol = symbol;
            this.count = count;
            this.win = win;
        }

        public static WinningLineDto from(com.casino.platform.game.service.SlotGameEngine.WinningLine winningLine) {
            return new WinningLineDto(
                    winningLine.getLineIndex(),
                    winningLine.getSymbol(),
                    winningLine.getCount(),
                    winningLine.getWin()
            );
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public BigDecimal getWin() {
            return win;
        }

        public void setWin(BigDecimal win) {
            this.win = win;
        }
    }
}
