package com.casino.platform.game.service;

import com.casino.platform.game.entity.Game;
import com.casino.platform.game.entity.GameRound;
import com.casino.platform.game.entity.GameStatus;
import com.casino.platform.game.repository.GameRepository;
import com.casino.platform.game.repository.GameRoundRepository;
import com.casino.platform.user.entity.User;
import com.casino.platform.wallet.entity.Wallet;
import com.casino.platform.wallet.repository.WalletRepository;
import com.casino.platform.wallet.service.WalletService;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GameSessionService {

    private final GameRepository gameRepository;
    private final GameRoundRepository gameRoundRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final SlotGameEngine slotGameEngine;

    public GameSessionService(GameRepository gameRepository,
                              GameRoundRepository gameRoundRepository,
                              WalletRepository walletRepository,
                              WalletService walletService,
                              SlotGameEngine slotGameEngine) {
        this.gameRepository = gameRepository;
        this.gameRoundRepository = gameRoundRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.slotGameEngine = slotGameEngine;
    }

    @Transactional
    @Retryable(maxAttempts = 3)
    public GamePlayResult playSlotGame(User user, Long gameId, BigDecimal betPerLine) {
        // Validate game
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new IllegalStateException("Game is not active");
        }

        // Calculate total bet (25 lines)
        BigDecimal totalBet = betPerLine.multiply(BigDecimal.valueOf(25));

        // Validate bet amount
        if (totalBet.compareTo(game.getMinBet()) < 0) {
            throw new IllegalArgumentException("Bet amount is below minimum: " + game.getMinBet());
        }
        if (totalBet.compareTo(game.getMaxBet()) > 0) {
            throw new IllegalArgumentException("Bet amount exceeds maximum: " + game.getMaxBet());
        }

        // Get wallet
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        BigDecimal balanceBefore = wallet.getBalance();

        // Check balance
        if (wallet.getBalance().compareTo(totalBet) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        // Deduct bet
        String betReferenceId = "BET-" + UUID.randomUUID();
        String betDescription = "Slot game bet - " + game.getName();
        walletService.placeBet(wallet.getId(), totalBet, betDescription, betReferenceId);

        // Spin reels and calculate win
        SlotGameEngine.SpinResult spinResult = slotGameEngine.spin(betPerLine);
        BigDecimal winAmount = spinResult.getTotalWin();

        // Credit win if any
        if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
            String winReferenceId = "WIN-" + UUID.randomUUID();
            String winDescription = "Slot game win - " + game.getName();
            walletService.creditWin(wallet.getId(), winAmount, winDescription, winReferenceId);
        }

        // Get updated balance
        wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        BigDecimal balanceAfter = wallet.getBalance();

        // Save game round
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("reels", spinResult.getReels());
        gameData.put("winningLines", spinResult.getWinningLines());
        gameData.put("betPerLine", betPerLine);

        GameRound gameRound = new GameRound(
                user,
                game,
                totalBet,
                winAmount,
                balanceBefore,
                balanceAfter,
                gameData
        );
        gameRoundRepository.save(gameRound);

        return new GamePlayResult(
                spinResult.getReels(),
                winAmount,
                balanceAfter,
                spinResult.getWinningLines()
        );
    }

    public static class GamePlayResult {
        private final String[][] reels;
        private final BigDecimal winAmount;
        private final BigDecimal balance;
        private final Object winningLines;

        public GamePlayResult(String[][] reels, BigDecimal winAmount, BigDecimal balance, Object winningLines) {
            this.reels = reels;
            this.winAmount = winAmount;
            this.balance = balance;
            this.winningLines = winningLines;
        }

        public String[][] getReels() {
            return reels;
        }

        public BigDecimal getWinAmount() {
            return winAmount;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public Object getWinningLines() {
            return winningLines;
        }
    }
}
