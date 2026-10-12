package com.casino.platform.provider.wallet.dto;

import java.math.BigDecimal;

/**
 * Common response DTO for all provider wallet operations.
 */
public class WalletResponse {

    private boolean success;
    private BigDecimal balance;
    private String currency;
    private String errorCode;
    private String errorMessage;

    // Constructors
    public WalletResponse() {
    }

    /**
     * Success response constructor.
     */
    public WalletResponse(BigDecimal balance, String currency) {
        this.success = true;
        this.balance = balance;
        this.currency = currency;
    }

    /**
     * Error response constructor.
     */
    public WalletResponse(String errorCode, String errorMessage) {
        this.success = false;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    // Static factory methods
    public static WalletResponse success(BigDecimal balance, String currency) {
        return new WalletResponse(balance, currency);
    }

    public static WalletResponse error(String errorCode, String errorMessage) {
        return new WalletResponse(errorCode, errorMessage);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
