package com.casino.platform.game.service;

import com.casino.platform.game.dto.LaunchGameResponse;
import com.casino.platform.game.entity.Game;
import com.casino.platform.provider.adapter.LaunchResult;
import com.casino.platform.provider.adapter.ProviderAdapter;
import com.casino.platform.provider.adapter.ProviderAdapterFactory;
import com.casino.platform.provider.entity.GameSession;
import com.casino.platform.provider.entity.PlayerProviderMapping;
import com.casino.platform.provider.entity.Provider;
import com.casino.platform.provider.repository.GameSessionRepository;
import com.casino.platform.provider.service.ProviderConfigService;
import com.casino.platform.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service for launching games and creating game sessions.
 */
@Service
public class GameLaunchService {

    private static final Logger log = LoggerFactory.getLogger(GameLaunchService.class);
    private static final String DEFAULT_PROVIDER_CODE = "DEMO";

    private final GameService gameService;
    private final ProviderConfigService providerConfigService;
    private final GameSessionRepository gameSessionRepository;
    private final ProviderAdapterFactory providerAdapterFactory;

    public GameLaunchService(
            GameService gameService,
            ProviderConfigService providerConfigService,
            GameSessionRepository gameSessionRepository,
            ProviderAdapterFactory providerAdapterFactory) {
        this.gameService = gameService;
        this.providerConfigService = providerConfigService;
        this.gameSessionRepository = gameSessionRepository;
        this.providerAdapterFactory = providerAdapterFactory;
    }

    /**
     * Launch a game for a user.
     * Creates a game session with authentication token.
     *
     * @param user Authenticated user
     * @param gameId Game ID to launch
     * @param currency Currency (default: DEMO)
     * @return Launch response with URL and session token
     */
    @Transactional
    public LaunchGameResponse launchGame(User user, Long gameId, String currency) {
        // Validate game
        Game game = gameService.getGameById(gameId);
        if (game.getStatus() != com.casino.platform.game.entity.GameStatus.ACTIVE) {
            throw new IllegalStateException("Game is not active: " + gameId);
        }

        // Get or create provider (currently only DEMO)
        Provider provider = providerConfigService.getProviderByCode(DEFAULT_PROVIDER_CODE);

        // Get or create player provider mapping
        PlayerProviderMapping mapping = providerConfigService.getOrCreatePlayerMapping(user, provider);
        log.info("Player provider mapping: userId={}, providerId={}, providerPlayerId={}",
                user.getId(), provider.getId(), mapping.getProviderPlayerId());

        // Generate session token (plain text, returned to client)
        String sessionToken = generateSessionToken();

        // Hash session token for storage (SHA-256)
        String sessionTokenHash = hashSessionToken(sessionToken);

        // Create game session
        GameSession session = new GameSession(user, provider, game, sessionTokenHash);
        session.setCurrency(currency);
        session = gameSessionRepository.save(session);

        log.info("Game session created: sessionId={}, userId={}, gameId={}, provider={}",
                session.getId(), user.getId(), gameId, provider.getCode());

        // Get provider adapter and launch game
        ProviderAdapter adapter = providerAdapterFactory.getAdapter(provider.getCode());
        LaunchResult launchResult = adapter.launch(user, game, session);

        // Append session token to launch URL
        String launchUrl = appendSessionToken(launchResult.getLaunchUrl(), sessionToken);

        return LaunchGameResponse.of(launchUrl, sessionToken, session.getId());
    }

    /**
     * Generate unique session token (UUID).
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Hash session token with SHA-256 for secure storage.
     */
    private String hashSessionToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash session token", e);
        }
    }

    /**
     * Append session token to launch URL.
     * Handles both URLs with and without existing query parameters.
     */
    private String appendSessionToken(String launchUrl, String sessionToken) {
        if (launchUrl.contains("?")) {
            return launchUrl + "&session=" + sessionToken;
        } else {
            return launchUrl + "?session=" + sessionToken;
        }
    }

    /**
     * Get game session by token hash.
     */
    @Transactional(readOnly = true)
    public GameSession getSessionByToken(String sessionToken) {
        String tokenHash = hashSessionToken(sessionToken);
        return gameSessionRepository.findBySessionTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session token"));
    }

    /**
     * Validate and get active session.
     */
    @Transactional(readOnly = true)
    public GameSession getActiveSession(String sessionToken) {
        String tokenHash = hashSessionToken(sessionToken);
        GameSession session = gameSessionRepository.findBySessionTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session token"));

        if (!session.isActive()) {
            throw new IllegalStateException("Session is not active or expired");
        }

        return session;
    }

    /**
     * Close game session.
     */
    @Transactional
    public void closeSession(Long sessionId) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.close();
        gameSessionRepository.save(session);
        log.info("Game session closed: sessionId={}", sessionId);
    }
}
