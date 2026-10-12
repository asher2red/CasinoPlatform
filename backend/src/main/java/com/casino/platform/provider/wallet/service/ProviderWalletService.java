package com.casino.platform.provider.wallet.service;

import com.casino.platform.game.entity.Game;
import com.casino.platform.game.repository.GameRepository;
import com.casino.platform.ledger.entity.Ledger;
import com.casino.platform.ledger.repository.LedgerRepository;
import com.casino.platform.provider.entity.*;
import com.casino.platform.provider.exception.ProviderErrorCode;
import com.casino.platform.provider.exception.ProviderNotFoundException;
import com.casino.platform.provider.repository.PlayerProviderMappingRepository;
import com.casino.platform.provider.repository.ProviderTransactionRepository;
import com.casino.platform.provider.rollback.RollbackIntentService;
import com.casino.platform.provider.service.ProviderConfigService;
import com.casino.platform.provider.wallet.dto.WalletResponse;
import com.casino.platform.user.entity.User;
import com.casino.platform.wallet.exception.InsufficientBalanceException;
import com.casino.platform.wallet.exception.WalletNotFoundException;
import com.casino.platform.wallet.service.WalletCoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Provider Wallet API service.
 * Handles balance, bet, win operations with idempotency and transaction recording.
 */
@Service
public class ProviderWalletService {

    private static final Logger log = LoggerFactory.getLogger(ProviderWalletService.class);

    private final ProviderConfigService providerConfigService;
    private final PlayerProviderMappingRepository mappingRepository;
    private final ProviderTransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final WalletCoreService walletCoreService;
    private final GameRepository gameRepository;
    private final RollbackIntentService rollbackIntentService;
    private final ObjectMapper objectMapper;

    public ProviderWalletService(
            ProviderConfigService providerConfigService,
            PlayerProviderMappingRepository mappingRepository,
            ProviderTransactionRepository transactionRepository,
            LedgerRepository ledgerRepository,
            WalletCoreService walletCoreService,
            GameRepository gameRepository,
            RollbackIntentService rollbackIntentService,
            ObjectMapper objectMapper) {
        this.providerConfigService = providerConfigService;
        this.mappingRepository = mappingRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.walletCoreService = walletCoreService;
        this.gameRepository = gameRepository;
        this.rollbackIntentService = rollbackIntentService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get player balance.
     */
    @Transactional(readOnly = true)
    public WalletResponse getBalance(Provider provider, String providerPlayerId, String currency) {
        try {
            // Validate currency
            if (!"DEMO".equals(currency)) {
                return buildErrorResponse(ProviderErrorCode.UNSUPPORTED_CURRENCY);
            }

            // Find user by provider player ID
            User user = findUserByProviderPlayerId(provider, providerPlayerId);

            // Get balance
            BigDecimal balance = walletCoreService.getBalance(user.getId());

            return WalletResponse.success(balance, currency);

        } catch (ProviderNotFoundException e) {
            log.warn("Player not found: {}", providerPlayerId);
            return buildErrorResponse(ProviderErrorCode.PLAYER_NOT_FOUND);
        } catch (WalletNotFoundException e) {
            log.warn("Wallet not found for player: {}", providerPlayerId);
            return buildErrorResponse(ProviderErrorCode.PLAYER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Error getting balance for player: {}", providerPlayerId, e);
            return buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
        }
    }

    /**
     * Process bet (debit) transaction.
     */
    @Transactional
    public WalletResponse processBet(
            Provider provider,
            String transactionId,
            String providerPlayerId,
            String currency,
            String gameId,
            String roundId,
            BigDecimal amount,
            String requestBody) {

        try {
            // Validate currency
            if (!"DEMO".equals(currency)) {
                return buildErrorResponse(ProviderErrorCode.UNSUPPORTED_CURRENCY);
            }

            // Validate amount
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return buildErrorResponse(ProviderErrorCode.INVALID_AMOUNT);
            }

            // Find user
            User user = findUserByProviderPlayerId(provider, providerPlayerId);

            // Check for duplicate transaction (idempotency)
            Optional<ProviderTransaction> existingTx = transactionRepository
                    .findByProviderIdAndTransactionId(provider.getId(), transactionId);

            if (existingTx.isPresent()) {
                ProviderTransaction tx = existingTx.get();
                if (tx.isSuccess()) {
                    // Return existing successful result
                    log.info("Duplicate bet transaction, returning existing result: {}", transactionId);
                    return WalletResponse.success(tx.getBalanceAfter(), currency);
                } else {
                    // Return existing error
                    return buildErrorResponse(tx.getErrorCode(), tx.getResponseBody());
                }
            }

            // Find game
            Game game = null;
            if (gameId != null) {
                try {
                    Long gameIdLong = Long.parseLong(gameId);
                    game = gameRepository.findById(gameIdLong).orElse(null);
                } catch (NumberFormatException e) {
                    log.warn("Invalid game ID format: {}", gameId);
                }
            }

            // Create pending transaction record
            ProviderTransaction transaction = new ProviderTransaction(
                    provider, transactionId, user, ProviderTransaction.OperationType.BET, currency);
            transaction.setAmount(amount);
            transaction.setGame(game);
            transaction.setRoundId(roundId);
            transaction.setRequestBody(requestBody);
            transaction = transactionRepository.save(transaction);

            try {
                // Process bet (debit)
                String referenceId = buildReferenceId(provider, transactionId);
                String description = String.format("Bet: %s (Round: %s)", transactionId, roundId);

                Ledger ledger = walletCoreService.debit(user.getId(), amount, referenceId, description);

                // Get updated balance
                BigDecimal balanceAfter = walletCoreService.getBalance(user.getId());

                // Update transaction as success
                transaction.setLedger(ledger);
                String responseBody = serializeResponse(WalletResponse.success(balanceAfter, currency));
                transaction.markSuccess(balanceAfter, responseBody);
                transactionRepository.save(transaction);

                // Link ledger back to provider transaction for audit trail
                ledger.setProviderTransaction(transaction);
                ledgerRepository.save(ledger);

                log.info("Bet processed: txId={}, amount={}, balance={}", transactionId, amount, balanceAfter);
                return WalletResponse.success(balanceAfter, currency);

            } catch (InsufficientBalanceException e) {
                log.warn("Insufficient balance for bet: txId={}, amount={}", transactionId, amount);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.INSUFFICIENT_FUNDS);
                transaction.markFailed(ProviderErrorCode.INSUFFICIENT_FUNDS.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(transaction);
                return errorResponse;

            } catch (IllegalStateException e) {
                log.error("Transaction conflict for bet: txId={}", transactionId, e);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.TRANSACTION_CONFLICT);
                transaction.markFailed(ProviderErrorCode.TRANSACTION_CONFLICT.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(transaction);
                return errorResponse;

            } catch (Exception e) {
                log.error("Error processing bet: txId={}", transactionId, e);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
                transaction.markFailed(ProviderErrorCode.RETRYABLE_ERROR.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(transaction);
                return errorResponse;
            }

        } catch (ProviderNotFoundException e) {
            log.warn("Player not found for bet: {}", providerPlayerId);
            return buildErrorResponse(ProviderErrorCode.PLAYER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Unexpected error in processBet: txId={}", transactionId, e);
            return buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
        }
    }

    /**
     * Process win (credit) transaction.
     */
    @Transactional
    public WalletResponse processWin(
            Provider provider,
            String transactionId,
            String providerPlayerId,
            String currency,
            String gameId,
            String roundId,
            BigDecimal amount,
            String originalTransactionId,
            String requestBody) {

        try {
            // Validate currency
            if (!"DEMO".equals(currency)) {
                return buildErrorResponse(ProviderErrorCode.UNSUPPORTED_CURRENCY);
            }

            // Validate amount
            if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
                return buildErrorResponse(ProviderErrorCode.INVALID_AMOUNT);
            }

            // Find user
            User user = findUserByProviderPlayerId(provider, providerPlayerId);

            // Check for duplicate transaction (idempotency)
            Optional<ProviderTransaction> existingTx = transactionRepository
                    .findByProviderIdAndTransactionId(provider.getId(), transactionId);

            if (existingTx.isPresent()) {
                ProviderTransaction tx = existingTx.get();
                if (tx.isSuccess()) {
                    // Return existing successful result
                    log.info("Duplicate win transaction, returning existing result: {}", transactionId);
                    return WalletResponse.success(tx.getBalanceAfter(), currency);
                } else {
                    // Return existing error
                    return buildErrorResponse(tx.getErrorCode(), tx.getResponseBody());
                }
            }

            // Find game
            Game game = null;
            if (gameId != null) {
                try {
                    Long gameIdLong = Long.parseLong(gameId);
                    game = gameRepository.findById(gameIdLong).orElse(null);
                } catch (NumberFormatException e) {
                    log.warn("Invalid game ID format: {}", gameId);
                }
            }

            // Create pending transaction record
            ProviderTransaction transaction = new ProviderTransaction(
                    provider, transactionId, user, ProviderTransaction.OperationType.WIN, currency);
            transaction.setAmount(amount);
            transaction.setGame(game);
            transaction.setRoundId(roundId);
            transaction.setOriginalTransactionId(originalTransactionId);
            transaction.setRequestBody(requestBody);
            transaction = transactionRepository.save(transaction);

            try {
                // Process win (credit)
                String referenceId = buildReferenceId(provider, transactionId);
                String description = String.format("Win: %s (Round: %s, Bet: %s)",
                        transactionId, roundId, originalTransactionId);

                Ledger ledger = walletCoreService.credit(user.getId(), amount, referenceId, description);

                // Get updated balance
                BigDecimal balanceAfter = walletCoreService.getBalance(user.getId());

                // Update transaction as success
                transaction.setLedger(ledger);
                String responseBody = serializeResponse(WalletResponse.success(balanceAfter, currency));
                transaction.markSuccess(balanceAfter, responseBody);
                transactionRepository.save(transaction);

                // Link ledger back to provider transaction for audit trail
                ledger.setProviderTransaction(transaction);
                ledgerRepository.save(ledger);

                log.info("Win processed: txId={}, amount={}, balance={}", transactionId, amount, balanceAfter);
                return WalletResponse.success(balanceAfter, currency);

            } catch (IllegalStateException e) {
                log.error("Transaction conflict for win: txId={}", transactionId, e);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.TRANSACTION_CONFLICT);
                transaction.markFailed(ProviderErrorCode.TRANSACTION_CONFLICT.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(transaction);
                return errorResponse;

            } catch (Exception e) {
                log.error("Error processing win: txId={}", transactionId, e);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
                transaction.markFailed(ProviderErrorCode.RETRYABLE_ERROR.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(transaction);
                return errorResponse;
            }

        } catch (ProviderNotFoundException e) {
            log.warn("Player not found for win: {}", providerPlayerId);
            return buildErrorResponse(ProviderErrorCode.PLAYER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Unexpected error in processWin: txId={}", transactionId, e);
            return buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
        }
    }

    /**
     * Process rollback transaction.
     * Handles both normal rollback and out-of-order rollback (rollback arrives before original).
     */
    @Transactional
    public WalletResponse processRollback(
            Provider provider,
            String transactionId,
            String providerPlayerId,
            String currency,
            String originalTransactionId,
            String requestBody) {

        try {
            // Validate currency
            if (!"DEMO".equals(currency)) {
                return buildErrorResponse(ProviderErrorCode.UNSUPPORTED_CURRENCY);
            }

            // Find user
            User user = findUserByProviderPlayerId(provider, providerPlayerId);

            // Check for duplicate rollback transaction (idempotency)
            Optional<ProviderTransaction> existingRollback = transactionRepository
                    .findByProviderIdAndTransactionId(provider.getId(), transactionId);

            if (existingRollback.isPresent()) {
                ProviderTransaction tx = existingRollback.get();
                if (tx.isSuccess()) {
                    // Return existing successful result
                    log.info("Duplicate rollback transaction, returning existing result: {}", transactionId);
                    return WalletResponse.success(tx.getBalanceAfter(), currency);
                } else {
                    // Return existing error
                    return buildErrorResponse(tx.getErrorCode(), tx.getResponseBody());
                }
            }

            // Find original transaction
            Optional<ProviderTransaction> originalTxOpt = transactionRepository
                    .findOriginalTransaction(provider.getId(), originalTransactionId);

            // Case 1: Original transaction not found - save rollback intent
            if (originalTxOpt.isEmpty()) {
                log.info("Original transaction not found, saving rollback intent: originalTxId={}, rollbackTxId={}",
                        originalTransactionId, transactionId);

                // Save rollback intent for future processing
                rollbackIntentService.saveRollbackIntent(
                        provider, transactionId, originalTransactionId, user);

                // Create rollback transaction record as SUCCESS
                // (we accept the rollback even though original hasn't arrived yet)
                ProviderTransaction rollbackTx = new ProviderTransaction(
                        provider, transactionId, user, ProviderTransaction.OperationType.ROLLBACK, currency);
                rollbackTx.setOriginalTransactionId(originalTransactionId);
                rollbackTx.setRequestBody(requestBody);

                BigDecimal currentBalance = walletCoreService.getBalance(user.getId());
                String responseBody = serializeResponse(WalletResponse.success(currentBalance, currency));
                rollbackTx.markSuccess(currentBalance, responseBody);
                transactionRepository.save(rollbackTx);

                log.info("Rollback intent saved successfully: rollbackTxId={}", transactionId);
                return WalletResponse.success(currentBalance, currency);
            }

            // Case 2: Original transaction found - process actual rollback
            ProviderTransaction originalTx = originalTxOpt.get();

            // Validate original transaction is successful
            if (!originalTx.isSuccess()) {
                log.warn("Original transaction was not successful, cannot rollback: {}", originalTransactionId);
                return buildErrorResponse(ProviderErrorCode.ORIGINAL_TRANSACTION_NOT_FOUND);
            }

            // Create rollback transaction record
            ProviderTransaction rollbackTx = new ProviderTransaction(
                    provider, transactionId, user, ProviderTransaction.OperationType.ROLLBACK, currency);
            rollbackTx.setOriginalTransactionId(originalTransactionId);
            rollbackTx.setRequestBody(requestBody);
            rollbackTx = transactionRepository.save(rollbackTx);

            try {
                // Get original ledger entry
                Ledger originalLedger = originalTx.getLedger();
                if (originalLedger == null) {
                    log.error("Original transaction has no ledger entry: {}", originalTransactionId);
                    WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
                    rollbackTx.markFailed(ProviderErrorCode.RETRYABLE_ERROR.getCode(), serializeResponse(errorResponse));
                    transactionRepository.save(rollbackTx);
                    return errorResponse;
                }

                // Process rollback through wallet core service
                String rollbackReferenceId = buildReferenceId(provider, transactionId);
                String description = String.format("Rollback: %s (Original: %s)",
                        transactionId, originalTransactionId);

                Ledger rollbackLedger = walletCoreService.rollback(
                        originalLedger, rollbackReferenceId, description);

                // Get updated balance
                BigDecimal balanceAfter = walletCoreService.getBalance(user.getId());

                // Update rollback transaction as success
                rollbackTx.setLedger(rollbackLedger);
                String responseBody = serializeResponse(WalletResponse.success(balanceAfter, currency));
                rollbackTx.markSuccess(balanceAfter, responseBody);
                transactionRepository.save(rollbackTx);

                // Link ledger back to provider transaction for audit trail
                rollbackLedger.setProviderTransaction(rollbackTx);
                ledgerRepository.save(rollbackLedger);

                log.info("Rollback processed: rollbackTxId={}, originalTxId={}, balance={}",
                        transactionId, originalTransactionId, balanceAfter);
                return WalletResponse.success(balanceAfter, currency);

            } catch (InsufficientBalanceException e) {
                log.warn("Insufficient balance for rollback: rollbackTxId={}", transactionId);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.INSUFFICIENT_FUNDS);
                rollbackTx.markFailed(ProviderErrorCode.INSUFFICIENT_FUNDS.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(rollbackTx);
                return errorResponse;

            } catch (IllegalStateException e) {
                log.error("Transaction conflict for rollback: rollbackTxId={}", transactionId, e);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.TRANSACTION_CONFLICT);
                rollbackTx.markFailed(ProviderErrorCode.TRANSACTION_CONFLICT.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(rollbackTx);
                return errorResponse;

            } catch (Exception e) {
                log.error("Error processing rollback: rollbackTxId={}", transactionId, e);
                WalletResponse errorResponse = buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
                rollbackTx.markFailed(ProviderErrorCode.RETRYABLE_ERROR.getCode(), serializeResponse(errorResponse));
                transactionRepository.save(rollbackTx);
                return errorResponse;
            }

        } catch (ProviderNotFoundException e) {
            log.warn("Player not found for rollback: {}", providerPlayerId);
            return buildErrorResponse(ProviderErrorCode.PLAYER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Unexpected error in processRollback: rollbackTxId={}", transactionId, e);
            return buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
        }
    }

    /**
     * Find user by provider player ID.
     */
    private User findUserByProviderPlayerId(Provider provider, String providerPlayerId) {
        PlayerProviderMapping mapping = mappingRepository
                .findByProviderIdAndProviderPlayerId(provider.getId(), providerPlayerId)
                .orElseThrow(() -> new ProviderNotFoundException("Player not found: " + providerPlayerId));
        return mapping.getUser();
    }

    /**
     * Build reference ID for ledger.
     * Format: {providerCode}-{transactionId}
     */
    private String buildReferenceId(Provider provider, String transactionId) {
        return provider.getCode() + "-" + transactionId;
    }

    /**
     * Serialize response to JSON string.
     */
    private String serializeResponse(WalletResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response", e);
            return "{}";
        }
    }

    /**
     * Build error response from error code.
     */
    private WalletResponse buildErrorResponse(ProviderErrorCode errorCode) {
        return WalletResponse.error(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * Build error response from stored error data.
     */
    private WalletResponse buildErrorResponse(String errorCode, String errorMessage) {
        if (errorCode == null) {
            return buildErrorResponse(ProviderErrorCode.RETRYABLE_ERROR);
        }
        return WalletResponse.error(errorCode, errorMessage);
    }
}
