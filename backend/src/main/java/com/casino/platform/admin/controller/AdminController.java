package com.casino.platform.admin.controller;

import com.casino.platform.admin.dto.UpdateUserStatusRequest;
import com.casino.platform.admin.dto.UserAdminResponse;
import com.casino.platform.admin.service.AdminService;
import com.casino.platform.game.entity.GameRound;
import com.casino.platform.game.entity.GameStatus;
import com.casino.platform.ledger.entity.Ledger;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // User Management
    @GetMapping("/users")
    public ResponseEntity<Page<UserAdminResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserAdminResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserAdminResponse> getUserById(@PathVariable Long userId) {
        UserAdminResponse user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        try {
            adminService.updateUserStatus(userId, request.getStatus());
            Map<String, String> response = new HashMap<>();
            response.put("message", "User status updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Transaction Management
    @GetMapping("/transactions")
    public ResponseEntity<Page<Ledger>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ledger> transactions = adminService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }

    // Game Round Management
    @GetMapping("/game-rounds")
    public ResponseEntity<Page<GameRound>> getAllGameRounds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GameRound> gameRounds = adminService.getAllGameRounds(pageable);
        return ResponseEntity.ok(gameRounds);
    }

    @GetMapping("/game-rounds/game/{gameId}")
    public ResponseEntity<Page<GameRound>> getGameRoundsByGame(
            @PathVariable Long gameId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GameRound> gameRounds = adminService.getGameRoundsByGameId(gameId, pageable);
        return ResponseEntity.ok(gameRounds);
    }

    // Game Management
    @GetMapping("/games")
    public ResponseEntity<?> getAllGames() {
        return ResponseEntity.ok(adminService.getAllGames());
    }

    @PutMapping("/games/{gameId}/status")
    public ResponseEntity<?> updateGameStatus(
            @PathVariable Long gameId,
            @RequestBody Map<String, String> request
    ) {
        try {
            GameStatus status = GameStatus.valueOf(request.get("status"));
            adminService.updateGameStatus(gameId, status);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Game status updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid status or game not found");
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Dashboard Statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        // TODO: Implement statistics aggregation
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 0);
        stats.put("activeUsers", 0);
        stats.put("totalGames", 0);
        stats.put("activeGames", 0);
        return ResponseEntity.ok(stats);
    }
}
