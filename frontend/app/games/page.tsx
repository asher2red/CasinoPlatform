"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
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

const CATEGORIES = [
  { value: "", label: "All Games" },
  { value: "SLOTS", label: "Slots" },
  { value: "CARDS", label: "Cards" },
  { value: "ROULETTE", label: "Roulette" },
  { value: "DICE", label: "Dice" },
];

const getCategoryColor = (category: string) => {
  switch (category) {
    case "SLOTS":
      return "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200";
    case "CARDS":
      return "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200";
    case "ROULETTE":
      return "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200";
    case "DICE":
      return "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200";
    default:
      return "bg-zinc-100 text-zinc-800 dark:bg-zinc-700 dark:text-zinc-200";
  }
};

export default function GamesPage() {
  const router = useRouter();
  const [games, setGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchGames(selectedCategory);
  }, [selectedCategory]);

  const fetchGames = async (category: string) => {
    setLoading(true);
    setError(null);

    try {
      const url = category
        ? `http://localhost:8080/api/games?category=${category}`
        : "http://localhost:8080/api/games";

      const response = await fetch(url, {
        credentials: "include",
      });

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          router.push("/login");
          return;
        }
        throw new Error("Failed to fetch games");
      }

      const data = await response.json();
      setGames(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load games");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
        <div className="text-zinc-600 dark:text-zinc-400">Loading games...</div>
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
                className="text-sm font-medium text-blue-600 dark:text-blue-400"
              >
                Games
              </Link>
              <Link
                href="/wallet"
                className="text-sm font-medium text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50"
              >
                Wallet
              </Link>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 space-y-6">
          {error && (
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
              <p className="text-red-800 dark:text-red-200 text-sm">{error}</p>
            </div>
          )}

          <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
            <h1 className="text-3xl font-bold text-zinc-900 dark:text-zinc-50 mb-4">
              Game Lobby
            </h1>
            <p className="text-zinc-600 dark:text-zinc-400 mb-6">
              Choose your game and start playing
            </p>

            <div className="flex gap-2 overflow-x-auto pb-2">
              {CATEGORIES.map((cat) => (
                <button
                  key={cat.value}
                  onClick={() => setSelectedCategory(cat.value)}
                  className={`px-4 py-2 rounded-lg font-medium whitespace-nowrap transition-colors ${
                    selectedCategory === cat.value
                      ? "bg-blue-600 text-white"
                      : "bg-zinc-100 text-zinc-700 hover:bg-zinc-200 dark:bg-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-600"
                  }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>
          </div>

          {games.length === 0 ? (
            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-8 text-center">
              <p className="text-zinc-600 dark:text-zinc-400">
                No games available in this category
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {games.map((game) => (
                <div
                  key={game.id}
                  className="bg-white dark:bg-zinc-800 rounded-lg shadow hover:shadow-lg transition-shadow overflow-hidden"
                >
                  <div className="h-48 bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                    <div className="text-6xl">🎰</div>
                  </div>

                  <div className="p-6">
                    <div className="flex items-start justify-between mb-2">
                      <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50">
                        {game.name}
                      </h3>
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded ${getCategoryColor(
                          game.category
                        )}`}
                      >
                        {game.category}
                      </span>
                    </div>

                    <p className="text-sm text-zinc-600 dark:text-zinc-400 mb-4">
                      {game.description}
                    </p>

                    <div className="flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400 mb-4">
                      <span>Min: ${game.minBet.toFixed(2)}</span>
                      <span>Max: ${game.maxBet.toFixed(2)}</span>
                    </div>

                    <button
                      onClick={() => router.push(`/games/${game.id}`)}
                      className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg transition-colors"
                    >
                      Play Now
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
