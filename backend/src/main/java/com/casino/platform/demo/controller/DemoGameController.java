package com.casino.platform.demo.controller;

import com.casino.platform.demo.dto.SpinRequest;
import com.casino.platform.demo.dto.SpinResponse;
import com.casino.platform.demo.entity.DemoRound;
import com.casino.platform.demo.service.DemoRoundService;
import com.casino.platform.game.service.GameLaunchService;
import com.casino.platform.game.service.SlotGameEngine.SpinResult;
import com.casino.platform.provider.entity.GameSession;
import com.casino.platform.provider.service.ProviderConfigService;
import com.casino.platform.wallet.service.WalletCoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Controller for demo game operations.
 * Handles game spin requests with session-based authentication.
 */
@RestController
@RequestMapping("/api/demo-provider/sessions")
public class DemoGameController {

    private static final Logger log = LoggerFactory.getLogger(DemoGameController.class);

    private final DemoRoundService demoRoundService;
    private final GameLaunchService gameLaunchService;
    private final ProviderConfigService providerConfigService;
    private final WalletCoreService walletCoreService;
    private final ObjectMapper objectMapper;

    public DemoGameController(
            DemoRoundService demoRoundService,
            GameLaunchService gameLaunchService,
            ProviderConfigService providerConfigService,
            WalletCoreService walletCoreService,
            ObjectMapper objectMapper) {
        this.demoRoundService = demoRoundService;
        this.gameLaunchService = gameLaunchService;
        this.providerConfigService = providerConfigService;
        this.walletCoreService = walletCoreService;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/demo-provider/sessions/{sessionToken}/spin
     * Spin the demo slot game.
     *
     * @param sessionToken Session token from game launch
     * @param idempotencyKey Idempotency key from header (required)
     * @param request Spin request with betPerLine
     * @return Spin result with reels, winAmount, balance
     */
    @PostMapping("/{sessionToken}/spin")
    public ResponseEntity<SpinResponse> spin(
            @PathVariable String sessionToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SpinRequest request) {

        log.info("Spin request: sessionToken={}, idempotencyKey={}, betPerLine={}",
                sessionToken, idempotencyKey, request.getBetPerLine());

        // Validate idempotency key
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Get active game session
        GameSession session;
        try {
            session = gameLaunchService.getActiveSession(sessionToken);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid or expired session: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }

        // Get provider (DEMO)
        var provider = providerConfigService.getProviderByCode("DEMO");

        // Play round (idempotent)
        DemoRound round;
        try {
            round = demoRoundService.playRound(
                    session.getUser(),
                    session,
                    provider,
                    request.getBetPerLine(),
                    idempotencyKey
            );
        } catch (IllegalStateException e) {
            log.error("Failed to play round: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }

        // Get current balance
        BigDecimal balance = walletCoreService.getBalance(session.getUser().getId());

        // Parse reel result from JSON
        SpinResult spinResult = parseSpinResult(round.getReelResult());

        // Build response
        SpinResponse response = SpinResponse.from(
                spinResult,
                balance,
                round.getRoundId(),
                round.getStatus().name()
        );

        log.info("Spin response: roundId={}, winAmount={}, balance={}",
                round.getRoundId(), round.getWinAmount(), balance);

        return ResponseEntity.ok(response);
    }

    /**
     * Parse spin result from JSON.
     */
    private SpinResult parseSpinResult(String json) {
        try {
            return objectMapper.readValue(json, SpinResult.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse spin result JSON", e);
            throw new IllegalStateException("Failed to parse spin result");
        }
    }
}
