"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/components/Toast";

interface Game {
  id: number;
  name: string;
  category: string;
  status: string;
  minBet: number;
  maxBet: number;
}

export default function AdminGames() {
  const router = useRouter();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [games, setGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user && user.role !== "ADMIN") {
      router.push("/dashboard");
    } else if (user) {
      fetchGames();
    }
  }, [user, router]);

  const fetchGames = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/admin/games", {
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Failed to fetch games");
      }

      const data = await response.json();
      setGames(data);
    } catch (err) {
      showToast("Failed to load games", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleStatusToggle = async (gameId: number, currentStatus: string) => {
    const newStatus = currentStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";

    try {
      const response = await fetch(
        `http://localhost:8080/api/admin/games/${gameId}/status`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify({ status: newStatus }),
        }
      );

      if (!response.ok) {
        throw new Error("Failed to update game status");
      }

      showToast(`Game ${newStatus === "ACTIVE" ? "activated" : "deactivated"}`, "success");
      fetchGames();
    } catch (err) {
      showToast("Failed to update game status", "error");
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
            Game Management
          </h1>

          <div className="bg-white dark:bg-zinc-800 rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-zinc-200 dark:divide-zinc-700">
              <thead className="bg-zinc-50 dark:bg-zinc-900">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Category
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Min Bet
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Max Bet
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-zinc-800 divide-y divide-zinc-200 dark:divide-zinc-700">
                {games.map((game) => (
                  <tr key={game.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      {game.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-zinc-900 dark:text-zinc-50">
                      {game.name}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-600 dark:text-zinc-400">
                      {game.category}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      ${game.minBet.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      ${game.maxBet.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded ${
                          game.status === "ACTIVE"
                            ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                            : "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
                        }`}
                      >
                        {game.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <button
                        onClick={() => handleStatusToggle(game.id, game.status)}
                        className={`font-medium ${
                          game.status === "ACTIVE"
                            ? "text-red-600 hover:text-red-800"
                            : "text-green-600 hover:text-green-800"
                        }`}
                      >
                        {game.status === "ACTIVE" ? "Deactivate" : "Activate"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </Layout>
    </ProtectedRoute>
  );
}
