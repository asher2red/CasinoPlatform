package com.casino.platform.game.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class SlotGameEngine {

    // Symbols
    private static final String SEVEN = "7️⃣";
    private static final String DIAMOND = "💎";
    private static final String BELL = "🔔";
    private static final String CHERRY = "🍒";
    private static final String LEMON = "🍋";
    private static final String GRAPE = "🍇";
    private static final String WATERMELON = "🍉";

    // Symbol weights (lower = rarer)
    private static final Map<String, Integer> SYMBOL_WEIGHTS = Map.of(
            SEVEN, 1,
            DIAMOND, 3,
            BELL, 5,
            CHERRY, 8,
            LEMON, 12,
            GRAPE, 12,
            WATERMELON, 12
    );

    // Paytable: symbol -> count -> multiplier
    private static final Map<String, Map<Integer, Integer>> PAYTABLE = new HashMap<>();
    static {
        PAYTABLE.put(SEVEN, Map.of(5, 100, 4, 50, 3, 20));
        PAYTABLE.put(DIAMOND, Map.of(5, 50, 4, 25, 3, 10));
        PAYTABLE.put(BELL, Map.of(5, 25, 4, 12, 3, 5));
        PAYTABLE.put(CHERRY, Map.of(5, 15, 4, 8, 3, 3));
        PAYTABLE.put(LEMON, Map.of(5, 10, 4, 5, 3, 2));
        PAYTABLE.put(GRAPE, Map.of(5, 10, 4, 5, 3, 2));
        PAYTABLE.put(WATERMELON, Map.of(5, 10, 4, 5, 3, 2));
    }

    // 25 Paylines (row, col positions)
    private static final int[][][] PAYLINES = {
            // Straight lines
            {{0,0}, {0,1}, {0,2}, {0,3}, {0,4}},
            {{1,0}, {1,1}, {1,2}, {1,3}, {1,4}},
            {{2,0}, {2,1}, {2,2}, {2,3}, {2,4}},
            // V shapes
            {{0,0}, {1,1}, {2,2}, {1,3}, {0,4}},
            {{2,0}, {1,1}, {0,2}, {1,3}, {2,4}},
            // Zigzag patterns
            {{0,0}, {1,1}, {0,2}, {1,3}, {0,4}},
            {{2,0}, {1,1}, {2,2}, {1,3}, {2,4}},
            {{1,0}, {0,1}, {1,2}, {0,3}, {1,4}},
            {{1,0}, {2,1}, {1,2}, {2,3}, {1,4}},
            // W and M shapes
            {{0,0}, {1,1}, {1,2}, {1,3}, {0,4}},
            {{2,0}, {1,1}, {1,2}, {1,3}, {2,4}},
            {{0,0}, {0,1}, {1,2}, {0,3}, {0,4}},
            {{2,0}, {2,1}, {1,2}, {2,3}, {2,4}},
            // Additional patterns
            {{0,0}, {1,1}, {2,2}, {2,3}, {2,4}},
            {{2,0}, {1,1}, {0,2}, {0,3}, {0,4}},
            {{0,0}, {0,1}, {0,2}, {1,3}, {2,4}},
            {{2,0}, {2,1}, {2,2}, {1,3}, {0,4}},
            {{1,0}, {1,1}, {0,2}, {1,3}, {1,4}},
            {{1,0}, {1,1}, {2,2}, {1,3}, {1,4}},
            {{0,0}, {2,1}, {0,2}, {2,3}, {0,4}},
            {{2,0}, {0,1}, {2,2}, {0,3}, {2,4}},
            // Extra lines to reach 25
            {{0,0}, {1,1}, {0,2}, {0,3}, {0,4}},
            {{2,0}, {1,1}, {2,2}, {2,3}, {2,4}},
            {{1,0}, {0,1}, {0,2}, {1,3}, {2,4}},
            {{1,0}, {2,1}, {2,2}, {1,3}, {0,4}}
    };

    private final Random random = new Random();

    public SpinResult spin(BigDecimal betPerLine) {
        // Generate 5x3 grid
        String[][] reels = new String[3][5];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                reels[row][col] = getRandomSymbol();
            }
        }

        // Calculate wins
        BigDecimal totalWin = BigDecimal.ZERO;
        List<WinningLine> winningLines = new ArrayList<>();

        for (int i = 0; i < PAYLINES.length; i++) {
            int[][] line = PAYLINES[i];
            WinningLine winLine = checkLine(reels, line, i, betPerLine);
            if (winLine != null) {
                winningLines.add(winLine);
                totalWin = totalWin.add(winLine.win);
            }
        }

        // Apply RTP adjustment (95%)
        totalWin = adjustForRTP(totalWin, betPerLine.multiply(BigDecimal.valueOf(25)));

        return new SpinResult(reels, totalWin, winningLines);
    }

    private String getRandomSymbol() {
        int totalWeight = SYMBOL_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = random.nextInt(totalWeight);

        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : SYMBOL_WEIGHTS.entrySet()) {
            cumulative += entry.getValue();
            if (randomValue < cumulative) {
                return entry.getKey();
            }
        }

        return WATERMELON;
    }

    private WinningLine checkLine(String[][] reels, int[][] line, int lineIndex, BigDecimal betPerLine) {
        String firstSymbol = reels[line[0][0]][line[0][1]];
        int count = 1;

        for (int i = 1; i < line.length; i++) {
            String symbol = reels[line[i][0]][line[i][1]];
            if (symbol.equals(firstSymbol)) {
                count++;
            } else {
                break;
            }
        }

        if (count >= 3) {
            Map<Integer, Integer> symbolPayouts = PAYTABLE.get(firstSymbol);
            if (symbolPayouts != null && symbolPayouts.containsKey(count)) {
                int multiplier = symbolPayouts.get(count);
                BigDecimal win = betPerLine.multiply(BigDecimal.valueOf(multiplier));
                return new WinningLine(lineIndex, firstSymbol, count, multiplier, win);
            }
        }

        return null;
    }

    private BigDecimal adjustForRTP(BigDecimal baseWin, BigDecimal totalBet) {
        double targetRTP = 0.95;
        double randomFactor = random.nextDouble();

        if (randomFactor < targetRTP) {
            return baseWin;
        } else {
            double reduction = 0.1 + random.nextDouble() * 0.2;
            return baseWin.multiply(BigDecimal.valueOf(1 - reduction))
                    .setScale(2, BigDecimal.ROUND_DOWN);
        }
    }

    public static class SpinResult {
        private final String[][] reels;
        private final BigDecimal totalWin;
        private final List<WinningLine> winningLines;

        public SpinResult(String[][] reels, BigDecimal totalWin, List<WinningLine> winningLines) {
            this.reels = reels;
            this.totalWin = totalWin;
            this.winningLines = winningLines;
        }

        public String[][] getReels() {
            return reels;
        }

        public BigDecimal getTotalWin() {
            return totalWin;
        }

        public List<WinningLine> getWinningLines() {
            return winningLines;
        }
    }

    public static class WinningLine {
        private final int lineIndex;
        private final String symbol;
        private final int count;
        private final int multiplier;
        private final BigDecimal win;

        public WinningLine(int lineIndex, String symbol, int count, int multiplier, BigDecimal win) {
            this.lineIndex = lineIndex;
            this.symbol = symbol;
            this.count = count;
            this.multiplier = multiplier;
            this.win = win;
        }

        public int getLineIndex() {
            return lineIndex;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getCount() {
            return count;
        }

        public int getMultiplier() {
            return multiplier;
        }

        public BigDecimal getWin() {
            return win;
        }
    }
}
