"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/components/Toast";

interface Transaction {
  id: number;
  wallet: {
    id: number;
    user: {
      username: string;
    };
  };
  transactionType: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export default function AdminTransactions() {
  const router = useRouter();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (user && user.role !== "ADMIN") {
      router.push("/dashboard");
    } else if (user) {
      fetchTransactions();
    }
  }, [user, router, page]);

  const fetchTransactions = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/admin/transactions?page=${page}&size=50`,
        {
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error("Failed to fetch transactions");
      }

      const data = await response.json();
      setTransactions(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      showToast("Failed to load transactions", "error");
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
            Transaction History
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
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Amount
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Balance Before
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Balance After
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Date
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-zinc-800 divide-y divide-zinc-200 dark:divide-zinc-700">
                {transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      {tx.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-zinc-900 dark:text-zinc-50">
                      {tx.wallet.user.username}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded ${
                          tx.transactionType === "DEPOSIT"
                            ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                            : tx.transactionType === "GAME_WIN"
                            ? "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                            : "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
                        }`}
                      >
                        {tx.transactionType}
                      </span>
                    </td>
                    <td
                      className={`px-6 py-4 whitespace-nowrap text-sm font-medium ${
                        tx.amount >= 0
                          ? "text-green-600 dark:text-green-400"
                          : "text-red-600 dark:text-red-400"
                      }`}
                    >
                      {tx.amount >= 0 ? "+" : ""}${tx.amount.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      ${tx.balanceBefore.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      ${tx.balanceAfter.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-600 dark:text-zinc-400">
                      {new Date(tx.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
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
