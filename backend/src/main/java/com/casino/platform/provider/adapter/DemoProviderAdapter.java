package com.casino.platform.provider.adapter;

import com.casino.platform.game.entity.Game;
import com.casino.platform.provider.entity.GameSession;
import com.casino.platform.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provider adapter for DEMO provider.
 * Handles game launches for internal demo games.
 */
@Component
public class DemoProviderAdapter implements ProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(DemoProviderAdapter.class);
    private static final String PROVIDER_CODE = "DEMO";

    @Override
    public LaunchResult launch(User user, Game game, GameSession session) {
        log.info("Launching demo game: userId={}, gameId={}, sessionId={}",
                user.getId(), game.getId(), session.getId());

        // Build launch URL for demo game
        // The frontend will use this URL to load the game with the session token
        String launchUrl = buildDemoGameUrl(game, session);

        log.info("Demo game launch URL generated: {}", launchUrl);

        return LaunchResult.of(launchUrl, String.valueOf(game.getId()));
    }

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean supports(Game game) {
        // DEMO provider supports all games by default
        // In a real implementation, you might check game.getProviderId() or game.getProviderGameId()
        return true;
    }

    /**
     * Build demo game URL.
     * Format: /demo-game?session={sessionToken}&game={gameId}
     *
     * Note: We cannot access the plain session token here as it's not stored.
     * The token will be appended by the caller (GameLaunchService).
     */
    private String buildDemoGameUrl(Game game, GameSession session) {
        // Return a template URL
        // The actual session token will be appended by GameLaunchService
        return String.format("/demo-game?game=%d", game.getId());
    }
}
