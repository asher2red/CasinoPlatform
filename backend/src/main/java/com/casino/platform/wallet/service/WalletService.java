package com.casino.platform.wallet.service;

import com.casino.platform.ledger.entity.Ledger;
import com.casino.platform.ledger.entity.TransactionType;
import com.casino.platform.ledger.repository.LedgerRepository;
import com.casino.platform.wallet.entity.Wallet;
import com.casino.platform.wallet.repository.WalletRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    public WalletService(WalletRepository walletRepository, LedgerRepository ledgerRepository) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user"));
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUsername(String username) {
        return walletRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user"));
    }

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ledger deposit(Long walletId, BigDecimal amount, String description, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Check for duplicate transaction
        if (referenceId != null && ledgerRepository.existsByReferenceId(referenceId)) {
            throw new IllegalStateException("Duplicate transaction detected");
        }

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.updateBalance(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        walletRepository.save(wallet);

        Ledger ledger = new Ledger(
                wallet,
                TransactionType.DEPOSIT,
                amount,
                balanceBefore,
                balanceAfter,
                description,
                referenceId
        );

        return ledgerRepository.save(ledger);
    }

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ledger withdraw(Long walletId, BigDecimal amount, String description, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Check for duplicate transaction
        if (referenceId != null && ledgerRepository.existsByReferenceId(referenceId)) {
            throw new IllegalStateException("Duplicate transaction detected");
        }

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal balanceBefore = wallet.getBalance();

        // This will throw IllegalStateException if insufficient balance
        wallet.updateBalance(amount.negate());

        BigDecimal balanceAfter = wallet.getBalance();

        walletRepository.save(wallet);

        Ledger ledger = new Ledger(
                wallet,
                TransactionType.WITHDRAW,
                amount.negate(),
                balanceBefore,
                balanceAfter,
                description,
                referenceId
        );

        return ledgerRepository.save(ledger);
    }

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ledger placeBet(Long walletId, BigDecimal amount, String description, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bet amount must be positive");
        }

        // Check for duplicate transaction
        if (referenceId != null && ledgerRepository.existsByReferenceId(referenceId)) {
            throw new IllegalStateException("Duplicate bet detected");
        }

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal balanceBefore = wallet.getBalance();

        // This will throw IllegalStateException if insufficient balance
        wallet.updateBalance(amount.negate());

        BigDecimal balanceAfter = wallet.getBalance();

        walletRepository.save(wallet);

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

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ledger creditWin(Long walletId, BigDecimal amount, String description, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Win amount cannot be negative");
        }

        // Check for duplicate transaction
        if (referenceId != null && ledgerRepository.existsByReferenceId(referenceId)) {
            throw new IllegalStateException("Duplicate win credit detected");
        }

        Wallet wallet = walletRepository.findByIdWithLock(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.updateBalance(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        walletRepository.save(wallet);

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
}
