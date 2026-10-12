package com.casino.platform.admin.service;

import com.casino.platform.admin.dto.UserAdminResponse;
import com.casino.platform.game.entity.Game;
import com.casino.platform.game.entity.GameRound;
import com.casino.platform.game.entity.GameStatus;
import com.casino.platform.game.repository.GameRepository;
import com.casino.platform.game.repository.GameRoundRepository;
import com.casino.platform.ledger.entity.Ledger;
import com.casino.platform.ledger.repository.LedgerRepository;
import com.casino.platform.user.entity.User;
import com.casino.platform.user.entity.UserStatus;
import com.casino.platform.user.repository.UserRepository;
import com.casino.platform.wallet.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final GameRepository gameRepository;
    private final GameRoundRepository gameRoundRepository;

    public AdminService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        LedgerRepository ledgerRepository,
                        GameRepository gameRepository,
                        GameRoundRepository gameRoundRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.gameRepository = gameRepository;
        this.gameRoundRepository = gameRoundRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserAdminResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> {
            var wallet = walletRepository.findByUserId(user.getId()).orElse(null);
            return new UserAdminResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    wallet != null ? wallet.getBalance() : null,
                    user.getCreatedAt(),
                    null // lastLoginAt - not implemented yet
            );
        });
    }

    @Transactional(readOnly = true)
    public UserAdminResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        var wallet = walletRepository.findByUserId(user.getId()).orElse(null);

        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                wallet != null ? wallet.getBalance() : null,
                user.getCreatedAt(),
                null
        );
    }

    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<Ledger> getAllTransactions(Pageable pageable) {
        return ledgerRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameRound> getAllGameRounds(Pageable pageable) {
        return gameRoundRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameRound> getGameRoundsByGameId(Long gameId, Pageable pageable) {
        return gameRoundRepository.findByGameIdOrderByCreatedAtDesc(gameId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @Transactional
    public void updateGameStatus(Long gameId, GameStatus status) {
        var game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        game.setStatus(status);
        gameRepository.save(game);
    }
}
