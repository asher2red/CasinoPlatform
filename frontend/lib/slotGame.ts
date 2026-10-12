// Slot Game Configuration and Logic

export const SYMBOLS = {
  SEVEN: "7️⃣",
  DIAMOND: "💎",
  BELL: "🔔",
  CHERRY: "🍒",
  LEMON: "🍋",
  GRAPE: "🍇",
  WATERMELON: "🍉",
} as const;

export type Symbol = typeof SYMBOLS[keyof typeof SYMBOLS];

// Paytable: [symbol, count, multiplier]
export const PAYTABLE: Array<{ symbol: Symbol; count: number; multiplier: number }> = [
  // 7️⃣ - Highest paying
  { symbol: SYMBOLS.SEVEN, count: 5, multiplier: 100 },
  { symbol: SYMBOLS.SEVEN, count: 4, multiplier: 50 },
  { symbol: SYMBOLS.SEVEN, count: 3, multiplier: 20 },

  // 💎 - Second highest
  { symbol: SYMBOLS.DIAMOND, count: 5, multiplier: 50 },
  { symbol: SYMBOLS.DIAMOND, count: 4, multiplier: 25 },
  { symbol: SYMBOLS.DIAMOND, count: 3, multiplier: 10 },

  // 🔔
  { symbol: SYMBOLS.BELL, count: 5, multiplier: 25 },
  { symbol: SYMBOLS.BELL, count: 4, multiplier: 12 },
  { symbol: SYMBOLS.BELL, count: 3, multiplier: 5 },

  // 🍒
  { symbol: SYMBOLS.CHERRY, count: 5, multiplier: 15 },
  { symbol: SYMBOLS.CHERRY, count: 4, multiplier: 8 },
  { symbol: SYMBOLS.CHERRY, count: 3, multiplier: 3 },

  // Low paying symbols
  { symbol: SYMBOLS.LEMON, count: 5, multiplier: 10 },
  { symbol: SYMBOLS.LEMON, count: 4, multiplier: 5 },
  { symbol: SYMBOLS.LEMON, count: 3, multiplier: 2 },

  { symbol: SYMBOLS.GRAPE, count: 5, multiplier: 10 },
  { symbol: SYMBOLS.GRAPE, count: 4, multiplier: 5 },
  { symbol: SYMBOLS.GRAPE, count: 3, multiplier: 2 },

  { symbol: SYMBOLS.WATERMELON, count: 5, multiplier: 10 },
  { symbol: SYMBOLS.WATERMELON, count: 4, multiplier: 5 },
  { symbol: SYMBOLS.WATERMELON, count: 3, multiplier: 2 },
];

// 25 Paylines (5x3 grid, positions: [row][col])
// Line format: array of [row, col] positions
export const PAYLINES: number[][][] = [
  // Straight lines (5)
  [[0,0], [0,1], [0,2], [0,3], [0,4]], // Top
  [[1,0], [1,1], [1,2], [1,3], [1,4]], // Middle
  [[2,0], [2,1], [2,2], [2,3], [2,4]], // Bottom

  // V-shapes (2)
  [[0,0], [1,1], [2,2], [1,3], [0,4]], // V
  [[2,0], [1,1], [0,2], [1,3], [2,4]], // Inverted V

  // Zigzag patterns (6)
  [[0,0], [1,1], [0,2], [1,3], [0,4]], // Zigzag top
  [[2,0], [1,1], [2,2], [1,3], [2,4]], // Zigzag bottom
  [[1,0], [0,1], [1,2], [0,3], [1,4]], // Zigzag middle-top
  [[1,0], [2,1], [1,2], [2,3], [1,4]], // Zigzag middle-bottom

  // W and M shapes (4)
  [[0,0], [1,1], [1,2], [1,3], [0,4]], // M top
  [[2,0], [1,1], [1,2], [1,3], [2,4]], // W bottom
  [[0,0], [0,1], [1,2], [0,3], [0,4]], // Top with dip
  [[2,0], [2,1], [1,2], [2,3], [2,4]], // Bottom with bump

  // Additional patterns (8)
  [[0,0], [1,1], [2,2], [2,3], [2,4]],
  [[2,0], [1,1], [0,2], [0,3], [0,4]],
  [[0,0], [0,1], [0,2], [1,3], [2,4]],
  [[2,0], [2,1], [2,2], [1,3], [0,4]],
  [[1,0], [1,1], [0,2], [1,3], [1,4]],
  [[1,0], [1,1], [2,2], [1,3], [1,4]],
  [[0,0], [2,1], [0,2], [2,3], [0,4]],
  [[2,0], [0,1], [2,2], [0,3], [2,4]],
];

// Bet levels
export const BET_LEVELS = [0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50];

// Symbol weights for random generation (higher weight = more frequent)
const SYMBOL_WEIGHTS: Array<{ symbol: Symbol; weight: number }> = [
  { symbol: SYMBOLS.SEVEN, weight: 1 },      // Rarest
  { symbol: SYMBOLS.DIAMOND, weight: 3 },
  { symbol: SYMBOLS.BELL, weight: 5 },
  { symbol: SYMBOLS.CHERRY, weight: 8 },
  { symbol: SYMBOLS.LEMON, weight: 12 },
  { symbol: SYMBOLS.GRAPE, weight: 12 },
  { symbol: SYMBOLS.WATERMELON, weight: 12 },
];

// Generate random symbol based on weights
function getRandomSymbol(): Symbol {
  const totalWeight = SYMBOL_WEIGHTS.reduce((sum, s) => sum + s.weight, 0);
  let random = Math.random() * totalWeight;

  for (const { symbol, weight } of SYMBOL_WEIGHTS) {
    random -= weight;
    if (random <= 0) return symbol;
  }

  return SYMBOLS.WATERMELON;
}

// Generate random reel result (5x3 grid)
export function spinReels(): Symbol[][] {
  const result: Symbol[][] = [];

  for (let row = 0; row < 3; row++) {
    result[row] = [];
    for (let col = 0; col < 5; col++) {
      result[row][col] = getRandomSymbol();
    }
  }

  return result;
}

// Check a single line for wins
function checkLine(reels: Symbol[][], line: number[][]): { symbol: Symbol; count: number; multiplier: number } | null {
  const symbols = line.map(([row, col]) => reels[row][col]);
  const firstSymbol = symbols[0];

  // Count consecutive matching symbols from left
  let count = 1;
  for (let i = 1; i < symbols.length; i++) {
    if (symbols[i] === firstSymbol) {
      count++;
    } else {
      break;
    }
  }

  // Need at least 3 matching symbols
  if (count < 3) return null;

  // Find payout for this combination
  const payout = PAYTABLE.find(
    p => p.symbol === firstSymbol && p.count === count
  );

  return payout || null;
}

// Calculate total win for all lines
export function calculateWin(reels: Symbol[][], betPerLine: number): {
  totalWin: number;
  winningLines: Array<{ lineIndex: number; symbol: Symbol; count: number; multiplier: number; win: number }>;
} {
  const winningLines: Array<{ lineIndex: number; symbol: Symbol; count: number; multiplier: number; win: number }> = [];
  let totalWin = 0;

  PAYLINES.forEach((line, index) => {
    const result = checkLine(reels, line);
    if (result) {
      const win = betPerLine * result.multiplier;
      winningLines.push({
        lineIndex: index,
        symbol: result.symbol,
        count: result.count,
        multiplier: result.multiplier,
        win,
      });
      totalWin += win;
    }
  });

  return { totalWin, winningLines };
}

// RTP adjustment: 95% target
// This is a simple demo implementation
export function adjustForRTP(baseWin: number, totalBet: number): number {
  const targetRTP = 0.95;
  const randomFactor = Math.random();

  // 95% of the time, return actual win
  // 5% of the time, slightly reduce win to maintain RTP
  if (randomFactor < targetRTP) {
    return baseWin;
  } else {
    // Reduce win by 10-30% in the unlucky 5%
    const reduction = 0.1 + Math.random() * 0.2;
    return Math.floor(baseWin * (1 - reduction));
  }
}
