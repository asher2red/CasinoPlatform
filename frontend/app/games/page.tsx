"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Layout from "@/components/Layout";

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
  { value: "", label: "All Games", icon: "🎮" },
  { value: "SLOTS", label: "Slots", icon: "🎰" },
  { value: "CARDS", label: "Cards", icon: "🃏" },
  { value: "ROULETTE", label: "Roulette", icon: "🎡" },
  { value: "DICE", label: "Dice", icon: "🎲" },
];

const getCategoryBadgeStyle = (category: string) => {
  switch (category) {
    case "SLOTS":
      return {
        backgroundColor: "var(--badge-slots-bg)",
        color: "var(--badge-slots-text)",
      };
    case "CARDS":
      return {
        backgroundColor: "var(--badge-cards-bg)",
        color: "var(--badge-cards-text)",
      };
    case "ROULETTE":
      return {
        backgroundColor: "var(--badge-roulette-bg)",
        color: "var(--badge-roulette-text)",
      };
    case "DICE":
      return {
        backgroundColor: "var(--badge-dice-bg)",
        color: "var(--badge-dice-text)",
      };
    default:
      return {
        backgroundColor: "var(--bg-elevated)",
        color: "var(--text-secondary)",
      };
  }
};

const getCategoryGradient = (category: string) => {
  switch (category) {
    case "SLOTS":
      return "linear-gradient(135deg, #A855F7 0%, #6B21A8 100%)";
    case "CARDS":
      return "linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%)";
    case "ROULETTE":
      return "linear-gradient(135deg, #EF4444 0%, #991B1B 100%)";
    case "DICE":
      return "linear-gradient(135deg, #10B981 0%, #065F46 100%)";
    default:
      return "linear-gradient(135deg, #F59E0B 0%, #B45309 100%)";
  }
};

export default function GamesPage() {
  const router = useRouter();
  const [games, setGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

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

  const filteredGames = games.filter((game) =>
    game.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <Layout>
      <div
        className="min-h-screen"
        style={{ backgroundColor: "var(--bg-base)" }}
      >
        <main className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="mb-8">
            <h1
              className="text-3xl sm:text-4xl font-bold mb-2"
              style={{ color: "var(--text-primary)" }}
            >
              Game Lobby
            </h1>
            <p style={{ color: "var(--text-secondary)" }}>
              Choose your game and start playing with virtual money
            </p>
          </div>

          {/* Search & Filters */}
          <div
            className="mb-6 p-6 rounded-xl"
            style={{
              backgroundColor: "var(--card-bg)",
              border: "1px solid var(--card-border)",
            }}
          >
            {/* Search Bar */}
            <div className="mb-4">
              <input
                type="text"
                placeholder="Search games..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-4 py-3 rounded-lg transition-colors"
                style={{
                  backgroundColor: "var(--input-bg)",
                  border: "1px solid var(--input-border)",
                  color: "var(--input-text)",
                  transition: "var(--transition-fast)",
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = "var(--input-border-focus)";
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = "var(--input-border)";
                }}
              />
            </div>

            {/* Category Filters */}
            <div className="flex gap-2 overflow-x-auto pb-2">
              {CATEGORIES.map((cat) => (
                <button
                  key={cat.value}
                  onClick={() => setSelectedCategory(cat.value)}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium whitespace-nowrap transition-all"
                  style={{
                    backgroundColor:
                      selectedCategory === cat.value
                        ? "var(--button-primary-bg)"
                        : "var(--button-secondary-bg)",
                    color:
                      selectedCategory === cat.value
                        ? "var(--button-primary-text)"
                        : "var(--button-secondary-text)",
                    border:
                      selectedCategory === cat.value
                        ? "none"
                        : "1px solid var(--button-secondary-border)",
                    transition: "var(--transition-fast)",
                    transform:
                      selectedCategory === cat.value ? "scale(1.05)" : "scale(1)",
                  }}
                  onMouseEnter={(e) => {
                    if (selectedCategory !== cat.value) {
                      e.currentTarget.style.backgroundColor =
                        "var(--button-secondary-bg-hover)";
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (selectedCategory !== cat.value) {
                      e.currentTarget.style.backgroundColor =
                        "var(--button-secondary-bg)";
                    }
                  }}
                >
                  <span>{cat.icon}</span>
                  <span>{cat.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div
              className="mb-6 p-4 rounded-lg"
              style={{
                backgroundColor: "rgba(239, 68, 68, 0.1)",
                border: "1px solid var(--color-error)",
                color: "var(--color-error)",
              }}
            >
              {error}
            </div>
          )}

          {/* Loading State */}
          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div
                  key={i}
                  className="rounded-xl overflow-hidden animate-pulse"
                  style={{
                    backgroundColor: "var(--card-bg)",
                    border: "1px solid var(--card-border)",
                  }}
                >
                  <div
                    className="h-48"
                    style={{ backgroundColor: "var(--bg-elevated)" }}
                  />
                  <div className="p-6 space-y-3">
                    <div
                      className="h-4 rounded"
                      style={{ backgroundColor: "var(--bg-elevated)" }}
                    />
                    <div
                      className="h-3 rounded w-2/3"
                      style={{ backgroundColor: "var(--bg-elevated)" }}
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : filteredGames.length === 0 ? (
            <div
              className="text-center py-16 rounded-xl"
              style={{
                backgroundColor: "var(--card-bg)",
                border: "1px solid var(--card-border)",
              }}
            >
              <div className="text-6xl mb-4">🎮</div>
              <p
                className="text-lg"
                style={{ color: "var(--text-secondary)" }}
              >
                {searchQuery
                  ? "No games match your search"
                  : "No games available in this category"}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredGames.map((game) => (
                <div
                  key={game.id}
                  className="rounded-xl overflow-hidden transition-all cursor-pointer"
                  style={{
                    backgroundColor: "var(--game-card-bg)",
                    border: "1px solid var(--card-border)",
                    transition: "var(--transition-base)",
                  }}
                  onClick={() => router.push(`/games/${game.id}`)}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.backgroundColor =
                      "var(--game-card-hover)";
                    e.currentTarget.style.transform = "translateY(-4px)";
                    e.currentTarget.style.boxShadow = "var(--shadow-xl)";
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor =
                      "var(--game-card-bg)";
                    e.currentTarget.style.transform = "translateY(0)";
                    e.currentTarget.style.boxShadow = "none";
                  }}
                >
                  {/* Thumbnail */}
                  <div
                    className="flex items-center justify-center"
                    style={{
                      height: "var(--game-card-thumbnail-height)",
                      background: getCategoryGradient(game.category),
                    }}
                  >
                    <div className="text-6xl">
                      {CATEGORIES.find((c) => c.value === game.category)?.icon ||
                        "🎰"}
                    </div>
                  </div>

                  {/* Content */}
                  <div className="p-6">
                    <div className="flex items-start justify-between mb-3">
                      <h3
                        className="text-lg font-bold"
                        style={{ color: "var(--text-primary)" }}
                      >
                        {game.name}
                      </h3>
                      <span
                        className="px-2 py-1 text-xs font-semibold rounded-full"
                        style={getCategoryBadgeStyle(game.category)}
                      >
                        {game.category}
                      </span>
                    </div>

                    <p
                      className="text-sm mb-4 line-clamp-2"
                      style={{ color: "var(--text-secondary)" }}
                    >
                      {game.description}
                    </p>

                    <div
                      className="flex items-center justify-between text-xs mb-4 pb-4"
                      style={{
                        color: "var(--text-tertiary)",
                        borderBottom: "1px solid var(--border-subtle)",
                      }}
                    >
                      <div>
                        <span style={{ color: "var(--text-secondary)" }}>
                          Min Bet:{" "}
                        </span>
                        <span
                          className="font-semibold"
                          style={{ color: "var(--color-success)" }}
                        >
                          ${game.minBet.toFixed(2)}
                        </span>
                      </div>
                      <div>
                        <span style={{ color: "var(--text-secondary)" }}>
                          Max Bet:{" "}
                        </span>
                        <span
                          className="font-semibold"
                          style={{ color: "var(--color-primary)" }}
                        >
                          ${game.maxBet.toFixed(2)}
                        </span>
                      </div>
                    </div>

                    <button
                      className="w-full py-3 rounded-lg font-semibold transition-all"
                      style={{
                        backgroundColor: "var(--button-primary-bg)",
                        color: "var(--button-primary-text)",
                        transition: "var(--transition-fast)",
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.backgroundColor =
                          "var(--button-primary-bg-hover)";
                        e.currentTarget.style.transform = "scale(1.02)";
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.backgroundColor =
                          "var(--button-primary-bg)";
                        e.currentTarget.style.transform = "scale(1)";
                      }}
                      onClick={(e) => {
                        e.stopPropagation();
                        router.push(`/games/${game.id}`);
                      }}
                    >
                      Play Now
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </main>
      </div>
    </Layout>
  );
}
