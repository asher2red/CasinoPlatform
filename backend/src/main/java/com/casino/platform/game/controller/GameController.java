package com.casino.platform.game.controller;

import com.casino.platform.game.dto.GameResponse;
import com.casino.platform.game.dto.LaunchGameRequest;
import com.casino.platform.game.dto.LaunchGameResponse;
import com.casino.platform.game.entity.Game;
import com.casino.platform.game.entity.GameCategory;
import com.casino.platform.game.service.GameLaunchService;
import com.casino.platform.game.service.GameService;
import com.casino.platform.user.entity.User;
import com.casino.platform.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final GameLaunchService gameLaunchService;
    private final UserRepository userRepository;

    public GameController(
            GameService gameService,
            GameLaunchService gameLaunchService,
            UserRepository userRepository) {
        this.gameService = gameService;
        this.gameLaunchService = gameLaunchService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<GameResponse>> getGames(
            @RequestParam(required = false) String category) {

        List<Game> games;

        if (category != null && !category.isEmpty()) {
            try {
                GameCategory gameCategory = GameCategory.valueOf(category.toUpperCase());
                games = gameService.getGamesByCategory(gameCategory);
            } catch (IllegalArgumentException e) {
                games = gameService.getAllActiveGames();
            }
        } else {
            games = gameService.getAllActiveGames();
        }

        List<GameResponse> response = games.stream()
                .map(GameResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameResponse> getGameById(@PathVariable Long id) {
        Game game = gameService.getGameById(id);
        return ResponseEntity.ok(GameResponse.from(game));
    }

    /**
     * POST /api/games/{id}/launch
     * Launch a game and create a game session.
     */
    @PostMapping("/{id}/launch")
    public ResponseEntity<LaunchGameResponse> launchGame(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LaunchGameRequest request) {

        // Get authenticated user
        User user = getCurrentUser();

        // Default currency if not provided
        String currency = (request != null && request.getCurrency() != null)
                ? request.getCurrency()
                : "DEMO";

        // Launch game
        LaunchGameResponse response = gameLaunchService.launchGame(user, id, currency);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}
