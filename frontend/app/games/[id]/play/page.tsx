"use client";

import { useState, useEffect, Suspense } from "react";
import { useParams, useRouter } from "next/navigation";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useToast } from "@/components/Toast";
import {
  SYMBOLS,
  Symbol,
  PAYTABLE,
  BET_LEVELS,
} from "@/lib/slotGame";

interface Game {
  id: number;
  name: string;
  category: string;
  minBet: number;
  maxBet: number;
}

interface Wallet {
  balance: number;
}

function SlotGameContent() {
  const params = useParams();
  const router = useRouter();
  const { showToast } = useToast();
  const gameId = params.id;

  const [game, setGame] = useState<Game | null>(null);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [loading, setLoading] = useState(true);

  // Game state
  const [betLevelIndex, setBetLevelIndex] = useState(2); // Start at 0.5
  const [reels, setReels] = useState<Symbol[][]>([
    [SYMBOLS.CHERRY, SYMBOLS.LEMON, SYMBOLS.GRAPE, SYMBOLS.BELL, SYMBOLS.CHERRY],
    [SYMBOLS.LEMON, SYMBOLS.GRAPE, SYMBOLS.WATERMELON, SYMBOLS.CHERRY, SYMBOLS.LEMON],
    [SYMBOLS.GRAPE, SYMBOLS.BELL, SYMBOLS.LEMON, SYMBOLS.GRAPE, SYMBOLS.WATERMELON],
  ]);
  const [spinning, setSpinning] = useState(false);
  const [lastWin, setLastWin] = useState(0);
  const [showPaytable, setShowPaytable] = useState(false);

  const betPerLine = BET_LEVELS[betLevelIndex];
  const totalBet = betPerLine * 25; // 25 lines

  useEffect(() => {
    fetchGame();
    fetchWallet();
  }, [gameId]);

  const fetchGame = async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/games/${gameId}`, {
        credentials: "include",
      });
      if (response.ok) {
        const data = await response.json();
        setGame(data);
      }
    } catch (err) {
      showToast("Failed to load game", "error");
    } finally {
      setLoading(false);
    }
  };

  const fetchWallet = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/wallet", {
        credentials: "include",
      });
      if (response.ok) {
        const data = await response.json();
        setWallet(data);
      }
    } catch (err) {
      console.error("Failed to fetch wallet:", err);
    }
  };

  const handleSpin = async () => {
    if (!wallet) return;

    // Check balance
    if (wallet.balance < totalBet) {
      showToast(`Insufficient balance! You need $${totalBet.toFixed(2)} to spin.`, "error");
      return;
    }

    setSpinning(true);
    setLastWin(0);

    try {
      console.log("Sending spin request:", { gameId, betPerLine, totalBet });

      const response = await fetch(`http://localhost:8080/api/games/${gameId}/play`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          betPerLine: betPerLine,
        }),
      });

      console.log("Response status:", response.status, response.statusText);

      if (!response.ok) {
        const errorData = await response.json();
        console.error("Play game error response:", errorData);
        throw new Error(errorData.error || "Failed to play game");
      }

      const result = await response.json();
      console.log("Spin result:", result);

      // Update UI with server results
      setReels(result.reels);
      setLastWin(result.winAmount);
      setWallet({ balance: result.balance });

      if (result.winAmount > 0) {
        showToast(`You won $${result.winAmount.toFixed(2)}!`, "success");
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to spin", "error");
    } finally {
      setSpinning(false);
    }
  };

  const handleBetUp = () => {
    if (betLevelIndex < BET_LEVELS.length - 1) {
      setBetLevelIndex(betLevelIndex + 1);
    }
  };

  const handleBetDown = () => {
    if (betLevelIndex > 0) {
      setBetLevelIndex(betLevelIndex - 1);
    }
  };

  if (loading || !game) {
    return (
      <ProtectedRoute>
        <Layout>
          <div className="flex items-center justify-center min-h-screen">
            <div className="text-zinc-600 dark:text-zinc-400">Loading game...</div>
          </div>
        </Layout>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <Layout>
        <div className="max-w-6xl mx-auto py-6 px-4">
          {/* Game Header */}
          <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-4 mb-4">
            <div className="flex justify-between items-center">
              <div>
                <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
                  {game.name}
                </h1>
                <p className="text-sm text-zinc-600 dark:text-zinc-400">
                  5x3 Slot • 25 Lines • RTP 95%
                </p>
              </div>
              <button
                onClick={() => router.push("/games")}
                className="px-4 py-2 bg-zinc-600 hover:bg-zinc-700 text-white rounded-lg text-sm"
              >
                Exit Game
              </button>
            </div>
          </div>

          {/* Game Container */}
          <div className="bg-gradient-to-b from-purple-900 to-blue-900 rounded-lg shadow-2xl p-8">
            {/* Balance Display */}
            <div className="flex justify-between mb-6 text-white">
              <div className="bg-black/30 px-6 py-3 rounded-lg">
                <div className="text-sm opacity-80">Balance</div>
                <div className="text-2xl font-bold">${wallet?.balance.toFixed(2) || "0.00"}</div>
              </div>
              <div className="bg-black/30 px-6 py-3 rounded-lg">
                <div className="text-sm opacity-80">Last Win</div>
                <div className="text-2xl font-bold text-yellow-400">${lastWin.toFixed(2)}</div>
              </div>
            </div>

            {/* Slot Reels (5x3) */}
            <div className="bg-black/40 p-6 rounded-lg mb-6">
              {reels.map((row, rowIndex) => (
                <div key={rowIndex} className="flex justify-center gap-2 mb-2">
                  {row.map((symbol, colIndex) => (
                    <div
                      key={colIndex}
                      className="w-24 h-24 bg-white rounded-lg flex items-center justify-center text-5xl shadow-lg"
                    >
                      {symbol}
                    </div>
                  ))}
                </div>
              ))}
            </div>

            {/* Controls */}
            <div className="flex flex-wrap justify-center gap-4 items-center">
              {/* Paytable Button */}
              <button
                onClick={() => setShowPaytable(true)}
                className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg"
              >
                Paytable
              </button>

              {/* Bet Controls */}
              <div className="flex items-center gap-2 bg-black/30 px-4 py-2 rounded-lg">
                <button
                  onClick={handleBetDown}
                  disabled={betLevelIndex === 0 || spinning}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-zinc-600 disabled:cursor-not-allowed text-white font-bold rounded"
                >
                  BET -
                </button>
                <div className="text-center px-4">
                  <div className="text-white text-sm">Total Bet</div>
                  <div className="text-yellow-400 text-xl font-bold">${totalBet.toFixed(2)}</div>
                  <div className="text-white text-xs opacity-70">${betPerLine.toFixed(2)} × 25</div>
                </div>
                <button
                  onClick={handleBetUp}
                  disabled={betLevelIndex === BET_LEVELS.length - 1 || spinning}
                  className="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-zinc-600 disabled:cursor-not-allowed text-white font-bold rounded"
                >
                  BET +
                </button>
              </div>

              {/* Spin Button */}
              <button
                onClick={handleSpin}
                disabled={spinning || !wallet || wallet.balance < totalBet}
                className="px-12 py-4 bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600 disabled:from-zinc-600 disabled:to-zinc-600 disabled:cursor-not-allowed text-white font-bold text-xl rounded-lg shadow-lg transform hover:scale-105 transition-transform"
              >
                {spinning ? "SPINNING..." : "SPIN"}
              </button>
            </div>
          </div>

          {/* Paytable Modal */}
          {showPaytable && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-white dark:bg-zinc-800 rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto p-6">
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
                    Paytable
                  </h2>
                  <button
                    onClick={() => setShowPaytable(false)}
                    className="text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300 text-2xl"
                  >
                    ✕
                  </button>
                </div>

                <div className="space-y-4">
                  <div>
                    <h3 className="text-lg font-semibold mb-2 text-zinc-900 dark:text-zinc-50">
                      Symbol Payouts (per line bet)
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {Object.entries(
                        PAYTABLE.reduce((acc, p) => {
                          if (!acc[p.symbol]) acc[p.symbol] = [];
                          acc[p.symbol].push(p);
                          return acc;
                        }, {} as Record<Symbol, typeof PAYTABLE>)
                      ).map(([symbol, payouts]) => (
                        <div key={symbol} className="bg-zinc-100 dark:bg-zinc-700 p-4 rounded-lg">
                          <div className="text-4xl mb-2">{symbol}</div>
                          {payouts.map((p, i) => (
                            <div key={i} className="text-sm text-zinc-700 dark:text-zinc-300">
                              {p.count}x = {p.multiplier}x bet
                            </div>
                          ))}
                        </div>
                      ))}
                    </div>
                  </div>

                  <div>
                    <h3 className="text-lg font-semibold mb-2 text-zinc-900 dark:text-zinc-50">
                      Paylines
                    </h3>
                    <p className="text-zinc-700 dark:text-zinc-300 text-sm">
                      This game has 25 fixed paylines. Wins are paid for matching symbols on active lines
                      from left to right. Minimum 3 matching symbols required.
                    </p>
                  </div>

                  <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
                    <p className="text-sm text-blue-900 dark:text-blue-200">
                      <strong>RTP:</strong> 95% (Return to Player) - This is a demo game with simulated
                      returns for educational purposes only.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </Layout>
    </ProtectedRoute>
  );
}

export default function SlotGamePage() {
  return (
    <Suspense fallback={
      <ProtectedRoute>
        <Layout>
          <div className="flex items-center justify-center min-h-screen">
            <div className="text-zinc-600 dark:text-zinc-400">Loading game...</div>
          </div>
        </Layout>
      </ProtectedRoute>
    }>
      <SlotGameContent />
    </Suspense>
  );
}
