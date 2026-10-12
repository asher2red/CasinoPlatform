package com.casino.platform.provider.adapter;

import com.casino.platform.game.entity.Game;
import com.casino.platform.provider.entity.GameSession;
import com.casino.platform.user.entity.User;

/**
 * Interface for game provider adapters.
 * Each provider implementation must provide a way to launch games.
 */
public interface ProviderAdapter {

    /**
     * Launch a game for a user.
     *
     * @param user Authenticated user
     * @param game Game to launch
     * @param session Game session with authentication token
     * @return Launch result containing URL and metadata
     */
    LaunchResult launch(User user, Game game, GameSession session);

    /**
     * Get the provider code this adapter handles.
     *
     * @return Provider code (e.g., "DEMO", "EVOLUTION", "PRAGMATIC")
     */
    String getProviderCode();

    /**
     * Check if this adapter supports the given game.
     *
     * @param game Game to check
     * @return true if this adapter can launch the game
     */
    default boolean supports(Game game) {
        // Default implementation: check if game has provider metadata
        // Can be overridden by specific adapters
        return true;
    }
}
