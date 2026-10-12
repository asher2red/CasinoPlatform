"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useAuth } from "@/contexts/AuthContext";
import Link from "next/link";

export default function AdminDashboard() {
  const router = useRouter();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user && user.role !== "ADMIN") {
      router.push("/dashboard");
    } else if (user) {
      setLoading(false);
    }
  }, [user, router]);

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
            Admin Dashboard
          </h1>

          {/* Quick Stats */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
              <div className="text-sm text-zinc-600 dark:text-zinc-400 mb-1">Total Users</div>
              <div className="text-3xl font-bold text-zinc-900 dark:text-zinc-50">-</div>
            </div>
            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
              <div className="text-sm text-zinc-600 dark:text-zinc-400 mb-1">Active Games</div>
              <div className="text-3xl font-bold text-zinc-900 dark:text-zinc-50">7</div>
            </div>
            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
              <div className="text-sm text-zinc-600 dark:text-zinc-400 mb-1">Total Bets</div>
              <div className="text-3xl font-bold text-zinc-900 dark:text-zinc-50">-</div>
            </div>
            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
              <div className="text-sm text-zinc-600 dark:text-zinc-400 mb-1">Total Wins</div>
              <div className="text-3xl font-bold text-zinc-900 dark:text-zinc-50">-</div>
            </div>
          </div>

          {/* Management Links */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Link
              href="/admin/users"
              className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6 hover:shadow-lg transition-shadow"
            >
              <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mb-2">
                👥 User Management
              </h2>
              <p className="text-zinc-600 dark:text-zinc-400 text-sm">
                View and manage user accounts, status, and permissions
              </p>
            </Link>

            <Link
              href="/admin/transactions"
              className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6 hover:shadow-lg transition-shadow"
            >
              <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mb-2">
                💰 Transactions
              </h2>
              <p className="text-zinc-600 dark:text-zinc-400 text-sm">
                Monitor all wallet transactions and financial activity
              </p>
            </Link>

            <Link
              href="/admin/game-rounds"
              className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6 hover:shadow-lg transition-shadow"
            >
              <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mb-2">
                🎲 Game Rounds
              </h2>
              <p className="text-zinc-600 dark:text-zinc-400 text-sm">
                View game history, results, and betting patterns
              </p>
            </Link>

            <Link
              href="/admin/games"
              className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6 hover:shadow-lg transition-shadow"
            >
              <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mb-2">
                🎰 Game Management
              </h2>
              <p className="text-zinc-600 dark:text-zinc-400 text-sm">
                Configure games, enable/disable, and adjust settings
              </p>
            </Link>
          </div>
        </div>
      </Layout>
    </ProtectedRoute>
  );
}
