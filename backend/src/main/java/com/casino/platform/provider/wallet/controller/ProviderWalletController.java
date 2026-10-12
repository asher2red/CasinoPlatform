package com.casino.platform.provider.wallet.controller;

import com.casino.platform.provider.entity.Provider;
import com.casino.platform.provider.exception.ProviderErrorCode;
import com.casino.platform.provider.security.ProviderAuthenticationToken;
import com.casino.platform.provider.wallet.dto.*;
import com.casino.platform.provider.wallet.service.ProviderWalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Provider Wallet API Controller.
 * Handles balance, bet, win, and rollback operations for external game providers.
 *
 * Base path: /api/provider-wallet/v1/{providerCode}
 * Authentication: HMAC-SHA256 via ProviderAuthenticationFilter
 */
@RestController
@RequestMapping("/api/provider-wallet/v1/{providerCode}")
public class ProviderWalletController {

    private static final Logger log = LoggerFactory.getLogger(ProviderWalletController.class);

    private final ProviderWalletService walletService;
    private final ObjectMapper objectMapper;

    public ProviderWalletController(ProviderWalletService walletService, ObjectMapper objectMapper) {
        this.walletService = walletService;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /api/provider-wallet/v1/{providerCode}/balance
     * Get player balance.
     */
    @GetMapping("/balance")
    public ResponseEntity<WalletResponse> getBalance(
            @PathVariable String providerCode,
            @RequestParam String playerId,
            @RequestParam(defaultValue = "DEMO") String currency) {

        Provider provider = getAuthenticatedProvider();
        if (provider == null) {
            return ResponseEntity.status(401)
                    .body(WalletResponse.error("UNAUTHORIZED", "Authentication required"));
        }

        log.info("Balance request: provider={}, playerId={}", providerCode, playerId);
        WalletResponse response = walletService.getBalance(provider, playerId, currency);

        return buildResponse(response);
    }

    /**
     * POST /api/provider-wallet/v1/{providerCode}/bet
     * Debit player balance for bet.
     */
    @PostMapping("/bet")
    public ResponseEntity<WalletResponse> placeBet(
            @PathVariable String providerCode,
            @Valid @RequestBody BetRequest request) {

        Provider provider = getAuthenticatedProvider();
        if (provider == null) {
            return ResponseEntity.status(401)
                    .body(WalletResponse.error("UNAUTHORIZED", "Authentication required"));
        }

        log.info("Bet request: provider={}, txId={}, playerId={}, amount={}",
                providerCode, request.getTransactionId(), request.getPlayerId(), request.getAmount());

        String requestBody = serializeRequest(request);

        WalletResponse response = walletService.processBet(
                provider,
                request.getTransactionId(),
                request.getPlayerId(),
                request.getCurrency(),
                request.getGameId(),
                request.getRoundId(),
                request.getAmount(),
                requestBody
        );

        return buildResponse(response);
    }

    /**
     * POST /api/provider-wallet/v1/{providerCode}/win
     * Credit player balance for win.
     */
    @PostMapping("/win")
    public ResponseEntity<WalletResponse> creditWin(
            @PathVariable String providerCode,
            @Valid @RequestBody WinRequest request) {

        Provider provider = getAuthenticatedProvider();
        if (provider == null) {
            return ResponseEntity.status(401)
                    .body(WalletResponse.error("UNAUTHORIZED", "Authentication required"));
        }

        log.info("Win request: provider={}, txId={}, playerId={}, amount={}",
                providerCode, request.getTransactionId(), request.getPlayerId(), request.getAmount());

        String requestBody = serializeRequest(request);

        WalletResponse response = walletService.processWin(
                provider,
                request.getTransactionId(),
                request.getPlayerId(),
                request.getCurrency(),
                request.getGameId(),
                request.getRoundId(),
                request.getAmount(),
                request.getOriginalTransactionId(),
                requestBody
        );

        return buildResponse(response);
    }

    /**
     * POST /api/provider-wallet/v1/{providerCode}/rollback
     * Rollback a previous transaction.
     */
    @PostMapping("/rollback")
    public ResponseEntity<WalletResponse> rollback(
            @PathVariable String providerCode,
            @Valid @RequestBody RollbackRequest request) {

        Provider provider = getAuthenticatedProvider();
        if (provider == null) {
            return ResponseEntity.status(401)
                    .body(WalletResponse.error("UNAUTHORIZED", "Authentication required"));
        }

        log.info("Rollback request: provider={}, txId={}, originalTxId={}",
                providerCode, request.getTransactionId(), request.getOriginalTransactionId());

        String requestBody = serializeRequest(request);

        WalletResponse response = walletService.processRollback(
                provider,
                request.getTransactionId(),
                request.getPlayerId(),
                request.getCurrency(),
                request.getOriginalTransactionId(),
                requestBody
        );

        return buildResponse(response);
    }

    /**
     * Get authenticated provider from security context.
     */
    private Provider getAuthenticatedProvider() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ProviderAuthenticationToken) {
            return ((ProviderAuthenticationToken) authentication).getProvider();
        }
        return null;
    }

    /**
     * Build HTTP response based on wallet response.
     */
    private ResponseEntity<WalletResponse> buildResponse(WalletResponse response) {
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            // Map error code to HTTP status
            ProviderErrorCode errorCode = parseErrorCode(response.getErrorCode());
            return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
        }
    }

    /**
     * Parse error code string to enum.
     */
    private ProviderErrorCode parseErrorCode(String errorCodeStr) {
        try {
            return ProviderErrorCode.valueOf(errorCodeStr);
        } catch (Exception e) {
            return ProviderErrorCode.RETRYABLE_ERROR;
        }
    }

    /**
     * Serialize request to JSON string.
     */
    private String serializeRequest(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("Failed to serialize request", e);
            return "{}";
        }
    }
}
