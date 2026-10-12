"use client";

import { useState, useEffect } from "react";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useToast } from "@/components/Toast";
import { WalletSkeleton, ListSkeleton } from "@/components/Skeleton";

interface Wallet {
  id: number;
  userId: number;
  balance: number;
  currencyType: string;
  createdAt: string;
  updatedAt: string;
}

interface Transaction {
  id: number;
  walletId: number;
  transactionType: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description: string;
  referenceId: string;
  createdAt: string;
}

interface TransactionPage {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export default function WalletPage() {
  const { showToast } = useToast();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [depositAmount, setDepositAmount] = useState("1000");
  const [depositing, setDepositing] = useState(false);

  useEffect(() => {
    fetchWallet();
    fetchTransactions();
  }, []);

  const fetchWallet = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/wallet", {
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Failed to fetch wallet");
      }

      const data = await response.json();
      setWallet(data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to load wallet", "error");
    } finally {
      setLoading(false);
    }
  };

  const fetchTransactions = async () => {
    try {
      const response = await fetch(
        "http://localhost:8080/api/wallet/transactions?page=0&size=20",
        {
          credentials: "include",
        }
      );

      if (response.ok) {
        const data: TransactionPage = await response.json();
        setTransactions(data.content);
      }
    } catch (err) {
      console.error("Failed to fetch transactions:", err);
    }
  };

  const handleDeposit = async () => {
    const amount = parseFloat(depositAmount);
    if (isNaN(amount) || amount <= 0) {
      showToast("Please enter a valid amount", "warning");
      return;
    }

    setDepositing(true);

    try {
      const response = await fetch(
        "http://localhost:8080/api/wallet/dev/deposit",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify({
            amount: amount,
            description: "Development virtual money grant",
          }),
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Deposit failed");
      }

      showToast(`Successfully added $${amount.toFixed(2)}`, "success");
      await fetchWallet();
      await fetchTransactions();
      setDepositAmount("1000");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Deposit failed", "error");
    } finally {
      setDepositing(false);
    }
  };

  if (loading) {
    return (
      <ProtectedRoute>
        <Layout>
          <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
            <div className="px-4 space-y-6">
              <WalletSkeleton />
              <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
                <div className="h-6 bg-zinc-200 dark:bg-zinc-700 rounded w-48 mb-4"></div>
                <ListSkeleton count={3} />
              </div>
            </div>
          </div>
        </Layout>
      </ProtectedRoute>
    );
  }

  if (!wallet) {
    return (
      <ProtectedRoute>
        <Layout>
          <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
              <p className="text-red-800 dark:text-red-200">Failed to load wallet</p>
            </div>
          </div>
        </Layout>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <Layout>
        <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
          <div className="px-4 space-y-6">
            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
              <h2 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mb-4">
                My Wallet
              </h2>
              <div className="flex items-baseline gap-2">
                <span className="text-4xl font-bold text-zinc-900 dark:text-zinc-50">
                  {wallet.balance.toFixed(2)}
                </span>
                <span className="text-lg text-zinc-600 dark:text-zinc-400">
                  {wallet.currencyType}
                </span>
              </div>
            </div>

            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
              <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50 mb-4">
                Get Virtual Money (Dev Only)
              </h3>
              <div className="flex gap-4">
                <input
                  type="number"
                  value={depositAmount}
                  onChange={(e) => setDepositAmount(e.target.value)}
                  className="flex-1 px-4 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg dark:bg-zinc-700 dark:text-zinc-100"
                  placeholder="Enter amount"
                />
                <button
                  onClick={handleDeposit}
                  disabled={depositing}
                  className="px-6 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg transition-colors"
                >
                  {depositing ? "Processing..." : "Add Funds"}
                </button>
              </div>
            </div>

            <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6">
              <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50 mb-4">
                Transaction History
              </h3>
              {transactions.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-zinc-600 dark:text-zinc-400 mb-4">
                    No transactions yet
                  </p>
                  <p className="text-sm text-zinc-500 dark:text-zinc-500">
                    Add some virtual money to get started!
                  </p>
                </div>
              ) : (
                <div className="space-y-4">
                  {transactions.map((tx) => (
                    <div
                      key={tx.id}
                      className="flex justify-between items-center border-b border-zinc-200 dark:border-zinc-700 pb-4 last:border-0"
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <span
                            className={`px-2 py-1 text-xs font-medium rounded ${
                              tx.transactionType === "DEPOSIT"
                                ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                                : tx.transactionType === "GAME_BET"
                                ? "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
                                : "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                            }`}
                          >
                            {tx.transactionType}
                          </span>
                          <span className="text-sm text-zinc-600 dark:text-zinc-400">
                            {new Date(tx.createdAt).toLocaleString()}
                          </span>
                        </div>
                        <p className="text-sm text-zinc-700 dark:text-zinc-300 mt-1">
                          {tx.description}
                        </p>
                      </div>
                      <div className="text-right">
                        <p
                          className={`text-lg font-semibold ${
                            tx.amount >= 0
                              ? "text-green-600 dark:text-green-400"
                              : "text-red-600 dark:text-red-400"
                          }`}
                        >
                          {tx.amount >= 0 ? "+" : ""}
                          {tx.amount.toFixed(2)}
                        </p>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400">
                          Balance: {tx.balanceAfter.toFixed(2)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </Layout>
    </ProtectedRoute>
  );
}
