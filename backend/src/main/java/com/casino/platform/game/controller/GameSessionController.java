package com.casino.platform.game.controller;

import com.casino.platform.game.dto.PlayGameRequest;
import com.casino.platform.game.service.GameSessionService;
import com.casino.platform.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameSessionController {

    private final GameSessionService gameSessionService;
    private final UserRepository userRepository;

    public GameSessionController(GameSessionService gameSessionService, UserRepository userRepository) {
        this.gameSessionService = gameSessionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{gameId}/play")
    public ResponseEntity<?> playGame(
            @PathVariable Long gameId,
            @Valid @RequestBody PlayGameRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            // Get user from repository using authenticated username
            var user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            GameSessionService.GamePlayResult result = gameSessionService.playSlotGame(
                    user,
                    gameId,
                    request.getBetPerLine()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("reels", result.getReels());
            response.put("winAmount", result.getWinAmount());
            response.put("balance", result.getBalance());
            response.put("winningLines", result.getWinningLines());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process game: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
