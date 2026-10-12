"use client";

import { useState, useEffect, Suspense } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";

interface Game {
  id: number;
  name: string;
  category: string;
  description: string;
  thumbnailUrl: string | null;
  status: string;
  minBet: number;
  maxBet: number;
  createdAt: string;
}

function GameDetailContent() {
  const router = useRouter();
  const params = useParams();
  const gameId = params.id;

  const [game, setGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (gameId) {
      fetchGame(gameId as string);
    }
  }, [gameId]);

  const fetchGame = async (id: string) => {
    try {
      const response = await fetch(`http://localhost:8080/api/games/${id}`, {
        credentials: "include",
      });

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          router.push("/login");
          return;
        }
        throw new Error("Failed to fetch game");
      }

      const data = await response.json();
      setGame(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load game");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
        <div className="text-zinc-600 dark:text-zinc-400">Loading...</div>
      </div>
    );
  }

  if (error || !game) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
        <div className="text-center">
          <p className="text-red-600 mb-4">{error || "Game not found"}</p>
          <Link
            href="/games"
            className="text-blue-600 hover:text-blue-700 font-medium"
          >
            Back to Games
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-zinc-900">
      <nav className="bg-white dark:bg-zinc-800 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center gap-8">
              <Link
                href="/dashboard"
                className="text-xl font-bold text-zinc-900 dark:text-zinc-50"
              >
                Casino Platform
              </Link>
              <Link
                href="/games"
                className="text-sm font-medium text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50"
              >
                ← Back to Games
              </Link>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-4xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
        <div className="bg-white dark:bg-zinc-800 rounded-lg shadow-lg overflow-hidden">
          <div className="h-64 bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
            <div className="text-9xl">🎰</div>
          </div>

          <div className="p-8">
            <div className="flex items-start justify-between mb-6">
              <div>
                <h1 className="text-3xl font-bold text-zinc-900 dark:text-zinc-50 mb-2">
                  {game.name}
                </h1>
                <span className="px-3 py-1 text-sm font-medium rounded bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
                  {game.category}
                </span>
              </div>
            </div>

            <p className="text-zinc-700 dark:text-zinc-300 mb-6">
              {game.description}
            </p>

            <div className="grid grid-cols-2 gap-4 mb-8">
              <div className="bg-zinc-50 dark:bg-zinc-900 rounded-lg p-4">
                <p className="text-sm text-zinc-600 dark:text-zinc-400 mb-1">
                  Minimum Bet
                </p>
                <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
                  ${game.minBet.toFixed(2)}
                </p>
              </div>
              <div className="bg-zinc-50 dark:bg-zinc-900 rounded-lg p-4">
                <p className="text-sm text-zinc-600 dark:text-zinc-400 mb-1">
                  Maximum Bet
                </p>
                <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
                  ${game.maxBet.toFixed(2)}
                </p>
              </div>
            </div>

            <div className="flex gap-4">
              <Link
                href={`/games/${game.id}/play`}
                className="flex-1 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 text-white font-bold py-4 px-8 rounded-lg text-center transition-all shadow-lg hover:shadow-xl text-lg"
              >
                🎰 Play Game
              </Link>
              <Link
                href="/games"
                className="flex-1 bg-zinc-600 hover:bg-zinc-700 text-white font-medium py-3 px-6 rounded-lg text-center transition-colors"
              >
                Back to Lobby
              </Link>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default function GameDetailPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
        <div className="text-zinc-600 dark:text-zinc-400">Loading...</div>
      </div>
    }>
      <GameDetailContent />
    </Suspense>
  );
}
