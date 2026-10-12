"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  status: string;
  createdAt: string;
}

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchCurrentUser();
  }, []);

  const fetchCurrentUser = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/auth/me", {
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Not authenticated");
      }

      const userData = await response.json();
      setUser(userData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch user");
      router.push("/login");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await fetch("http://localhost:8080/api/auth/logout", {
        method: "POST",
        credentials: "include",
      });

      localStorage.removeItem("user");
      router.push("/");
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
        <div className="text-zinc-600 dark:text-zinc-400">Loading...</div>
      </div>
    );
  }

  if (error || !user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-zinc-900">
      <nav className="bg-white dark:bg-zinc-800 shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center gap-8">
              <h1 className="text-xl font-bold text-zinc-900 dark:text-zinc-50">
                Casino Platform
              </h1>
              <Link
                href="/games"
                className="text-sm font-medium text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50"
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
            <div className="flex items-center gap-4">
              <span className="text-sm text-zinc-600 dark:text-zinc-400">
                {user.username}
              </span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium transition-colors"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6 space-y-6">
            <div>
              <h2 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mb-4">
                Welcome, {user.username}!
              </h2>
              <p className="text-zinc-600 dark:text-zinc-400">
                You are successfully logged in.
              </p>
            </div>

            <div className="border-t border-zinc-200 dark:border-zinc-700 pt-6">
              <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50 mb-4">
                Account Information
              </h3>
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <dt className="text-sm font-medium text-zinc-500 dark:text-zinc-400">User ID</dt>
                  <dd className="mt-1 text-sm text-zinc-900 dark:text-zinc-100">{user.id}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Username</dt>
                  <dd className="mt-1 text-sm text-zinc-900 dark:text-zinc-100">{user.username}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Email</dt>
                  <dd className="mt-1 text-sm text-zinc-900 dark:text-zinc-100">{user.email}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Role</dt>
                  <dd className="mt-1 text-sm text-zinc-900 dark:text-zinc-100">
                    <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
                      {user.role}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Status</dt>
                  <dd className="mt-1 text-sm text-zinc-900 dark:text-zinc-100">
                    <span className="px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200">
                      {user.status}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Member Since</dt>
                  <dd className="mt-1 text-sm text-zinc-900 dark:text-zinc-100">
                    {new Date(user.createdAt).toLocaleDateString()}
                  </dd>
                </div>
              </dl>
            </div>

            <div className="border-t border-zinc-200 dark:border-zinc-700 pt-6">
              <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50 mb-4">
                Coming Soon
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Link href="/wallet" className="p-4 border border-zinc-200 dark:border-zinc-700 rounded-lg hover:border-blue-500 dark:hover:border-blue-500 transition-colors">
                  <h4 className="font-medium text-zinc-900 dark:text-zinc-100 mb-2">Wallet ✓</h4>
                  <p className="text-sm text-zinc-600 dark:text-zinc-400">
                    Manage your virtual currency
                  </p>
                </Link>
                <Link href="/games" className="p-4 border border-zinc-200 dark:border-zinc-700 rounded-lg hover:border-blue-500 dark:hover:border-blue-500 transition-colors">
                  <h4 className="font-medium text-zinc-900 dark:text-zinc-100 mb-2">Game Lobby ✓</h4>
                  <p className="text-sm text-zinc-600 dark:text-zinc-400">
                    Browse available games
                  </p>
                </Link>
                <div className="p-4 border border-zinc-200 dark:border-zinc-700 rounded-lg">
                  <h4 className="font-medium text-zinc-900 dark:text-zinc-100 mb-2">History</h4>
                  <p className="text-sm text-zinc-600 dark:text-zinc-400">
                    View your game history
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
