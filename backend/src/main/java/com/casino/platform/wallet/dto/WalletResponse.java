package com.casino.platform.wallet.dto;

import com.casino.platform.wallet.entity.CurrencyType;
import com.casino.platform.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private CurrencyType currencyType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public WalletResponse() {
    }

    public WalletResponse(Long id, Long userId, BigDecimal balance, CurrencyType currencyType,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.currencyType = currencyType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getBalance(),
                wallet.getCurrencyType(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    public void setCurrencyType(CurrencyType currencyType) {
        this.currencyType = currencyType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
