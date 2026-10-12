package com.casino.platform.demo.service;

import com.casino.platform.demo.entity.DemoRound;
import com.casino.platform.demo.repository.DemoRoundRepository;
import com.casino.platform.game.service.SlotGameEngine;
import com.casino.platform.game.service.SlotGameEngine.SpinResult;
import com.casino.platform.provider.entity.GameSession;
import com.casino.platform.provider.entity.Provider;
import com.casino.platform.provider.wallet.dto.BetRequest;
import com.casino.platform.provider.wallet.dto.WinRequest;
import com.casino.platform.provider.wallet.dto.WalletResponse;
import com.casino.platform.provider.wallet.service.ProviderWalletService;
import com.casino.platform.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing demo slot game rounds.
 * Integrates with Provider Wallet API for bet and win transactions.
 */
@Service
public class DemoRoundService {

    private static final Logger log = LoggerFactory.getLogger(DemoRoundService.class);
    private static final int PAYLINES = 25;

    private final DemoRoundRepository roundRepository;
    private final ProviderWalletService walletService;
    private final SlotGameEngine slotGameEngine;
    private final ObjectMapper objectMapper;

    public DemoRoundService(
            DemoRoundRepository roundRepository,
            ProviderWalletService walletService,
            SlotGameEngine slotGameEngine,
            ObjectMapper objectMapper) {
        this.roundRepository = roundRepository;
        this.walletService = walletService;
        this.slotGameEngine = slotGameEngine;
        this.objectMapper = objectMapper;
    }

    /**
     * Play a demo round (spin).
     * Handles the complete flow: bet -> spin -> win with idempotency.
     *
     * @param user Authenticated user
     * @param session Active game session
     * @param provider Provider (DEMO)
     * @param betPerLine Bet amount per line
     * @param idempotencyKey Unique key for idempotent requests
     * @return Completed demo round
     */
    @Transactional
    public DemoRound playRound(
            User user,
            GameSession session,
            Provider provider,
            BigDecimal betPerLine,
            String idempotencyKey) {

        // Check for existing round with same idempotency key
        Optional<DemoRound> existing = roundRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            DemoRound existingRound = existing.get();
            log.info("Returning existing round for idempotency key: {}, status: {}",
                    idempotencyKey, existingRound.getStatus());
            return existingRound;
        }

        // Calculate total bet (betPerLine * 25 paylines)
        BigDecimal totalBet = betPerLine.multiply(BigDecimal.valueOf(PAYLINES));

        // Generate round ID
        String roundId = generateRoundId();

        // Create round in BET_PENDING state
        DemoRound round = new DemoRound(user, session, roundId, idempotencyKey, totalBet);

        try {
            round = roundRepository.save(round);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created the same round
            log.info("Race condition detected for idempotency key: {}, fetching existing round", idempotencyKey);
            return roundRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Failed to retrieve round after race condition"));
        }

        log.info("Created demo round: roundId={}, userId={}, betAmount={}, status=BET_PENDING",
                roundId, user.getId(), totalBet);

        // Step 1: Place bet via Provider Wallet API
        String betTransactionId = generateTransactionId("bet");
        WalletResponse betResponse = placeBet(provider, user, session, round, betTransactionId, totalBet);

        if (!betResponse.isSuccess()) {
            log.error("Bet failed: roundId={}, error={}", roundId, betResponse.getErrorCode());
            round.markFailed();
            roundRepository.save(round);
            throw new IllegalStateException("Bet failed: " + betResponse.getErrorMessage());
        }

        round.markBetSuccess(betTransactionId);
        roundRepository.save(round);
        log.info("Bet success: roundId={}, txId={}, balance={}", roundId, betTransactionId, betResponse.getBalance());

        // Step 2: Spin and calculate result
        SpinResult spinResult = slotGameEngine.spin(betPerLine);
        BigDecimal winAmount = spinResult.getTotalWin();

        // Save reel result as JSON
        round.setReelResult(serializeSpinResult(spinResult));
        round.markWinPending();
        roundRepository.save(round);

        log.info("Spin complete: roundId={}, winAmount={}", roundId, winAmount);

        // Step 3: Credit win via Provider Wallet API
        String winTransactionId = generateTransactionId("win");
        WalletResponse winResponse = creditWin(
                provider, user, session, round, winTransactionId, winAmount, betTransactionId);

        if (!winResponse.isSuccess()) {
            log.error("Win credit failed: roundId={}, error={}", roundId, winResponse.getErrorCode());
            round.markFailed();
            roundRepository.save(round);
            throw new IllegalStateException("Win credit failed: " + winResponse.getErrorMessage());
        }

        round.markWinSuccess(winTransactionId, winAmount);
        roundRepository.save(round);
        log.info("Win credit success: roundId={}, txId={}, balance={}", roundId, winTransactionId, winResponse.getBalance());

        return round;
    }

    /**
     * Place bet via Provider Wallet API.
     */
    private WalletResponse placeBet(
            Provider provider,
            User user,
            GameSession session,
            DemoRound round,
            String transactionId,
            BigDecimal amount) {

        // Get provider player ID from session user mapping
        String providerPlayerId = getProviderPlayerId(user, provider);

        BetRequest request = new BetRequest(
                transactionId,
                providerPlayerId,
                "DEMO",
                String.valueOf(session.getGame().getId()),
                round.getRoundId(),
                amount
        );

        String requestBody = serializeRequest(request);

        return walletService.processBet(
                provider,
                transactionId,
                providerPlayerId,
                "DEMO",
                String.valueOf(session.getGame().getId()),
                round.getRoundId(),
                amount,
                requestBody
        );
    }

    /**
     * Credit win via Provider Wallet API.
     */
    private WalletResponse creditWin(
            Provider provider,
            User user,
            GameSession session,
            DemoRound round,
            String transactionId,
            BigDecimal amount,
            String originalTransactionId) {

        String providerPlayerId = getProviderPlayerId(user, provider);

        WinRequest request = new WinRequest(
                transactionId,
                providerPlayerId,
                "DEMO",
                String.valueOf(session.getGame().getId()),
                round.getRoundId(),
                amount,
                originalTransactionId
        );

        String requestBody = serializeRequest(request);

        return walletService.processWin(
                provider,
                transactionId,
                providerPlayerId,
                "DEMO",
                String.valueOf(session.getGame().getId()),
                round.getRoundId(),
                amount,
                originalTransactionId,
                requestBody
        );
    }

    /**
     * Generate unique round ID.
     */
    private String generateRoundId() {
        return "round-" + UUID.randomUUID().toString();
    }

    /**
     * Generate unique transaction ID.
     */
    private String generateTransactionId(String type) {
        return type + "-" + UUID.randomUUID().toString();
    }

    /**
     * Get provider player ID for user.
     * Format: DEMO-{userId}-{suffix}
     */
    private String getProviderPlayerId(User user, Provider provider) {
        // Simplified: in real implementation, this would come from PlayerProviderMapping
        return String.format("%s-%d", provider.getCode(), user.getId());
    }

    /**
     * Serialize spin result to JSON.
     */
    private String serializeSpinResult(SpinResult spinResult) {
        try {
            return objectMapper.writeValueAsString(spinResult);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize spin result", e);
            return "{}";
        }
    }

    /**
     * Serialize request to JSON.
     */
    private String serializeRequest(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request", e);
            return "{}";
        }
    }

    /**
     * Get round by idempotency key.
     */
    @Transactional(readOnly = true)
    public Optional<DemoRound> getRoundByIdempotencyKey(String idempotencyKey) {
        return roundRepository.findByIdempotencyKey(idempotencyKey);
    }

    /**
     * Get user rounds.
     */
    @Transactional(readOnly = true)
    public java.util.List<DemoRound> getUserRounds(Long userId) {
        return roundRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
