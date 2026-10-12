package com.casino.platform.wallet.service;

import com.casino.platform.ledger.entity.Ledger;
import com.casino.platform.ledger.entity.TransactionType;
import com.casino.platform.ledger.repository.LedgerRepository;
import com.casino.platform.wallet.entity.Wallet;
import com.casino.platform.wallet.exception.InsufficientBalanceException;
import com.casino.platform.wallet.exception.WalletNotFoundException;
import com.casino.platform.wallet.repository.WalletRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Core wallet service for Provider Wallet API.
 * Handles balance operations with proper locking and idempotency.
 */
@Service
public class WalletCoreService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    public WalletCoreService(WalletRepository walletRepository, LedgerRepository ledgerRepository) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Get wallet balance by user ID.
     * Read-only operation, no locking needed.
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        return wallet.getBalance();
    }

    /**
     * Get wallet by user ID with pessimistic lock.
     * Used for balance operations.
     */
    @Transactional
    public Wallet getWalletWithLock(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        // Reload with pessimistic lock
        return walletRepository.findByIdWithLock(wallet.getId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + wallet.getId()));
    }

    /**
     * Debit (subtract) amount from wallet.
     * Used for BET operations.
     *
     * @param userId User ID
     * @param amount Amount to debit (positive value)
     * @param referenceId Unique transaction reference ID for idempotency
     * @param description Transaction description
     * @return Created ledger entry
     * @throws InsufficientBalanceException if wallet balance is insufficient
     * @throws IllegalStateException if duplicate transaction detected
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ledger debit(Long userId, BigDecimal amount, String referenceId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        // Check for duplicate transaction (idempotency)
        if (referenceId != null && ledgerRepository.existsByReferenceId(referenceId)) {
            throw new IllegalStateException("Duplicate transaction: " + referenceId);
        }

        Wallet wallet = getWalletWithLock(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        // Check sufficient balance before update
        if (balanceBefore.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Required: " + amount + ", Available: " + balanceBefore);
        }

        // Debit balance
        try {
            wallet.updateBalance(amount.negate());
        } catch (IllegalStateException e) {
            throw new InsufficientBalanceException("Insufficient balance: " + e.getMessage());
        }

        BigDecimal balanceAfter = wallet.getBalance();
        walletRepository.save(wallet);

        // Create ledger entry
        Ledger ledger = new Ledger(
                wallet,
                TransactionType.GAME_BET,
                amount.negate(),
                balanceBefore,
                balanceAfter,
                description,
                referenceId
        );

        return ledgerRepository.save(ledger);
    }

    /**
     * Credit (add) amount to wallet.
     * Used for WIN operations.
     *
     * @param userId User ID
     * @param amount Amount to credit (positive value, can be zero for no-win)
     * @param referenceId Unique transaction reference ID for idempotency
     * @param description Transaction description
     * @return Created ledger entry
     * @throws IllegalStateException if duplicate transaction detected
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ledger credit(Long userId, BigDecimal amount, String referenceId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit amount cannot be negative");
        }

        // Check for duplicate transaction (idempotency)
        if (referenceId != null && ledgerRepository.existsByReferenceId(referenceId)) {
            throw new IllegalStateException("Duplicate transaction: " + referenceId);
        }

        Wallet wallet = getWalletWithLock(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        // Credit balance
        wallet.updateBalance(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        walletRepository.save(wallet);

        // Create ledger entry
        Ledger ledger = new Ledger(
                wallet,
                TransactionType.GAME_WIN,
                amount,
                balanceBefore,
                balanceAfter,
                description,
                referenceId
        );

        return ledgerRepository.save(ledger);
    }

    /**
     * Rollback a previous transaction.
     * Used for ROLLBACK operations.
     *
     * @param originalLedger Original ledger entry to rollback
     * @param rollbackReferenceId Unique rollback transaction reference ID
     * @param description Rollback description
     * @return Created rollback ledger entry
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ledger rollback(Ledger originalLedger, String rollbackReferenceId, String description) {
        if (originalLedger == null) {
            throw new IllegalArgumentException("Original ledger cannot be null");
        }

        // Check for duplicate rollback transaction
        if (rollbackReferenceId != null && ledgerRepository.existsByReferenceId(rollbackReferenceId)) {
            throw new IllegalStateException("Duplicate rollback transaction: " + rollbackReferenceId);
        }

        Long userId = originalLedger.getWallet().getUser().getId();
        Wallet wallet = getWalletWithLock(userId);

        BigDecimal balanceBefore = wallet.getBalance();

        // Reverse the original transaction
        BigDecimal reversalAmount = originalLedger.getAmount().negate();

        // Apply reversal (BET becomes credit, WIN becomes debit)
        try {
            wallet.updateBalance(reversalAmount);
        } catch (IllegalStateException e) {
            throw new InsufficientBalanceException("Cannot rollback: " + e.getMessage());
        }

        BigDecimal balanceAfter = wallet.getBalance();
        walletRepository.save(wallet);

        // Determine rollback transaction type
        TransactionType rollbackType = originalLedger.getTransactionType() == TransactionType.GAME_BET
                ? TransactionType.GAME_BET_ROLLBACK
                : TransactionType.GAME_WIN_ROLLBACK;

        // Create rollback ledger entry
        Ledger rollbackLedger = new Ledger(
                wallet,
                rollbackType,
                reversalAmount,
                balanceBefore,
                balanceAfter,
                description,
                rollbackReferenceId
        );

        return ledgerRepository.save(rollbackLedger);
    }

    /**
     * Find ledger entry by reference ID.
     */
    @Transactional(readOnly = true)
    public Ledger findLedgerByReferenceId(String referenceId) {
        return ledgerRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger not found for reference: " + referenceId));
    }

    /**
     * Check if transaction exists by reference ID.
     */
    @Transactional(readOnly = true)
    public boolean transactionExists(String referenceId) {
        return ledgerRepository.existsByReferenceId(referenceId);
    }
}
