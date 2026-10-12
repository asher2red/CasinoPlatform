package com.casino.platform.ledger.dto;

import com.casino.platform.ledger.entity.Ledger;
import com.casino.platform.ledger.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerResponse {

    private Long id;
    private Long walletId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private String referenceId;
    private LocalDateTime createdAt;

    // Constructors
    public LedgerResponse() {
    }

    public LedgerResponse(Long id, Long walletId, TransactionType transactionType, BigDecimal amount,
                          BigDecimal balanceBefore, BigDecimal balanceAfter, String description,
                          String referenceId, LocalDateTime createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    public static LedgerResponse from(Ledger ledger) {
        return new LedgerResponse(
                ledger.getId(),
                ledger.getWallet().getId(),
                ledger.getTransactionType(),
                ledger.getAmount(),
                ledger.getBalanceBefore(),
                ledger.getBalanceAfter(),
                ledger.getDescription(),
                ledger.getReferenceId(),
                ledger.getCreatedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
