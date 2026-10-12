"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/components/Toast";

interface GameRound {
  id: number;
  user: {
    username: string;
  };
  game: {
    name: string;
  };
  betAmount: number;
  winAmount: number;
  balanceBefore: number;
  balanceAfter: number;
  createdAt: string;
}

export default function AdminGameRounds() {
  const router = useRouter();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [gameRounds, setGameRounds] = useState<GameRound[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (user && user.role !== "ADMIN") {
      router.push("/dashboard");
    } else if (user) {
      fetchGameRounds();
    }
  }, [user, router, page]);

  const fetchGameRounds = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/admin/game-rounds?page=${page}&size=50`,
        {
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error("Failed to fetch game rounds");
      }

      const data = await response.json();
      setGameRounds(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      showToast("Failed to load game rounds", "error");
    } finally {
      setLoading(false);
    }
  };

  if (loading || !user || user.role !== "ADMIN") {
    return (
      <ProtectedRoute>
        <Layout>
          <div className="flex items-center justify-center min-h-screen">
            <div className="text-zinc-600 dark:text-zinc-400">Loading...</div>
          </div>
        </Layout>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <Layout>
        <div className="max-w-7xl mx-auto py-6 px-4">
          <h1 className="text-3xl font-bold text-zinc-900 dark:text-zinc-50 mb-6">
            Game Round History
          </h1>

          <div className="bg-white dark:bg-zinc-800 rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-zinc-200 dark:divide-zinc-700">
              <thead className="bg-zinc-50 dark:bg-zinc-900">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    User
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Game
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Bet
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Win
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Net P/L
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Date
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-zinc-800 divide-y divide-zinc-200 dark:divide-zinc-700">
                {gameRounds.map((round) => {
                  const netPL = round.winAmount - round.betAmount;
                  return (
                    <tr key={round.id}>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                        {round.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-zinc-900 dark:text-zinc-50">
                        {round.user.username}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-600 dark:text-zinc-400">
                        {round.game.name}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-red-600 dark:text-red-400">
                        ${round.betAmount.toFixed(2)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-green-600 dark:text-green-400">
                        ${round.winAmount.toFixed(2)}
                      </td>
                      <td
                        className={`px-6 py-4 whitespace-nowrap text-sm font-medium ${
                          netPL >= 0
                            ? "text-green-600 dark:text-green-400"
                            : "text-red-600 dark:text-red-400"
                        }`}
                      >
                        {netPL >= 0 ? "+" : ""}${netPL.toFixed(2)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-600 dark:text-zinc-400">
                        {new Date(round.createdAt).toLocaleString()}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="px-6 py-4 flex justify-between items-center bg-zinc-50 dark:bg-zinc-900">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 text-sm font-medium text-zinc-700 dark:text-zinc-300 bg-white dark:bg-zinc-800 rounded hover:bg-zinc-100 dark:hover:bg-zinc-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <span className="text-sm text-zinc-600 dark:text-zinc-400">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-4 py-2 text-sm font-medium text-zinc-700 dark:text-zinc-300 bg-white dark:bg-zinc-800 rounded hover:bg-zinc-100 dark:hover:bg-zinc-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        </div>
      </Layout>
    </ProtectedRoute>
  );
}
