"use client";

import { useState, useEffect } from "react";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useToast } from "@/components/Toast";

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

const getTransactionIcon = (type: string) => {
  switch (type) {
    case "DEPOSIT":
      return "💰";
    case "GAME_BET":
      return "🎲";
    case "GAME_WIN":
      return "🎉";
    case "REFUND":
      return "↩️";
    default:
      return "💳";
  }
};

const getTransactionColor = (type: string) => {
  switch (type) {
    case "DEPOSIT":
    case "GAME_WIN":
    case "REFUND":
      return {
        bg: "var(--color-success)",
        text: "var(--color-zinc-50)",
      };
    case "GAME_BET":
      return {
        bg: "var(--color-error)",
        text: "var(--color-zinc-50)",
      };
    default:
      return {
        bg: "var(--color-info)",
        text: "var(--color-zinc-50)",
      };
  }
};

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
      showToast(
        err instanceof Error ? err.message : "Failed to load wallet",
        "error"
      );
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
      showToast(
        err instanceof Error ? err.message : "Deposit failed",
        "error"
      );
    } finally {
      setDepositing(false);
    }
  };

  if (loading) {
    return (
      <ProtectedRoute>
        <Layout>
          <div
            className="min-h-screen"
            style={{ backgroundColor: "var(--bg-base)" }}
          >
            <div className="max-w-5xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
              <div className="space-y-6">
                {/* Skeleton */}
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="rounded-xl p-6 animate-pulse"
                    style={{
                      backgroundColor: "var(--card-bg)",
                      border: "1px solid var(--card-border)",
                    }}
                  >
                    <div
                      className="h-6 rounded w-1/3 mb-4"
                      style={{ backgroundColor: "var(--bg-elevated)" }}
                    />
                    <div
                      className="h-4 rounded w-2/3"
                      style={{ backgroundColor: "var(--bg-elevated)" }}
                    />
                  </div>
                ))}
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
          <div
            className="min-h-screen"
            style={{ backgroundColor: "var(--bg-base)" }}
          >
            <div className="max-w-5xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
              <div
                className="rounded-xl p-6"
                style={{
                  backgroundColor: "rgba(239, 68, 68, 0.1)",
                  border: "1px solid var(--color-error)",
                  color: "var(--color-error)",
                }}
              >
                Failed to load wallet
              </div>
            </div>
          </div>
        </Layout>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <Layout>
        <div
          className="min-h-screen"
          style={{ backgroundColor: "var(--bg-base)" }}
        >
          <div className="max-w-5xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
            {/* Header */}
            <div className="mb-8">
              <h1
                className="text-3xl sm:text-4xl font-bold mb-2"
                style={{ color: "var(--text-primary)" }}
              >
                💰 Wallet
              </h1>
              <p style={{ color: "var(--text-secondary)" }}>
                Manage your virtual currency
              </p>
            </div>

            <div className="space-y-6">
              {/* Balance Card */}
              <div
                className="rounded-xl p-8"
                style={{
                  backgroundColor: "var(--card-bg)",
                  border: "1px solid var(--card-border)",
                  boxShadow: "var(--shadow-lg)",
                }}
              >
                <div className="flex items-center justify-between mb-4">
                  <h2
                    className="text-lg font-medium"
                    style={{ color: "var(--text-secondary)" }}
                  >
                    Available Balance
                  </h2>
                  <span
                    className="px-3 py-1 rounded-full text-xs font-semibold"
                    style={{
                      backgroundColor: "var(--bg-elevated)",
                      color: "var(--text-secondary)",
                    }}
                  >
                    {wallet.currencyType}
                  </span>
                </div>
                <div className="flex items-baseline gap-2">
                  <span
                    className="text-5xl font-bold"
                    style={{ color: "var(--balance-text)" }}
                  >
                    ${wallet.balance.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Deposit Card */}
              <div
                className="rounded-xl p-6"
                style={{
                  backgroundColor: "var(--card-bg)",
                  border: "1px solid var(--card-border)",
                }}
              >
                <div className="flex items-center gap-2 mb-4">
                  <span className="text-xl">⚠️</span>
                  <h3
                    className="text-lg font-semibold"
                    style={{ color: "var(--text-primary)" }}
                  >
                    Get Virtual Money (Dev Only)
                  </h3>
                </div>
                <div className="flex flex-col sm:flex-row gap-3">
                  <input
                    type="number"
                    value={depositAmount}
                    onChange={(e) => setDepositAmount(e.target.value)}
                    className="flex-1 px-4 py-3 rounded-lg transition-colors"
                    style={{
                      backgroundColor: "var(--input-bg)",
                      border: "1px solid var(--input-border)",
                      color: "var(--input-text)",
                      transition: "var(--transition-fast)",
                    }}
                    placeholder="Enter amount"
                    onFocus={(e) => {
                      e.currentTarget.style.borderColor =
                        "var(--input-border-focus)";
                    }}
                    onBlur={(e) => {
                      e.currentTarget.style.borderColor = "var(--input-border)";
                    }}
                  />
                  <button
                    onClick={handleDeposit}
                    disabled={depositing}
                    className="px-8 py-3 rounded-lg font-semibold transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    style={{
                      backgroundColor: depositing
                        ? "var(--bg-elevated)"
                        : "var(--button-primary-bg)",
                      color: "var(--button-primary-text)",
                      transition: "var(--transition-fast)",
                    }}
                    onMouseEnter={(e) => {
                      if (!depositing) {
                        e.currentTarget.style.backgroundColor =
                          "var(--button-primary-bg-hover)";
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (!depositing) {
                        e.currentTarget.style.backgroundColor =
                          "var(--button-primary-bg)";
                      }
                    }}
                  >
                    {depositing ? "Processing..." : "Add Funds"}
                  </button>
                </div>
              </div>

              {/* Transaction History */}
              <div
                className="rounded-xl p-6"
                style={{
                  backgroundColor: "var(--card-bg)",
                  border: "1px solid var(--card-border)",
                }}
              >
                <h3
                  className="text-xl font-bold mb-6"
                  style={{ color: "var(--text-primary)" }}
                >
                  Transaction History
                </h3>

                {transactions.length === 0 ? (
                  <div className="text-center py-12">
                    <div className="text-6xl mb-4">📭</div>
                    <p
                      className="text-lg mb-2"
                      style={{ color: "var(--text-secondary)" }}
                    >
                      No transactions yet
                    </p>
                    <p
                      className="text-sm"
                      style={{ color: "var(--text-tertiary)" }}
                    >
                      Add some virtual money to get started!
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {transactions.map((tx) => (
                      <div
                        key={tx.id}
                        className="flex items-center gap-4 p-4 rounded-lg transition-colors"
                        style={{
                          backgroundColor: "var(--bg-elevated)",
                          border: "1px solid var(--border-subtle)",
                          transition: "var(--transition-fast)",
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.backgroundColor =
                            "var(--bg-overlay)";
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.backgroundColor =
                            "var(--bg-elevated)";
                        }}
                      >
                        {/* Icon */}
                        <div
                          className="w-12 h-12 rounded-full flex items-center justify-center text-xl flex-shrink-0"
                          style={{
                            backgroundColor: getTransactionColor(
                              tx.transactionType
                            ).bg,
                          }}
                        >
                          {getTransactionIcon(tx.transactionType)}
                        </div>

                        {/* Details */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <span
                              className="px-2 py-0.5 text-xs font-semibold rounded"
                              style={{
                                backgroundColor: getTransactionColor(
                                  tx.transactionType
                                ).bg,
                                color: getTransactionColor(tx.transactionType)
                                  .text,
                              }}
                            >
                              {tx.transactionType}
                            </span>
                            <span
                              className="text-xs"
                              style={{ color: "var(--text-tertiary)" }}
                            >
                              {new Date(tx.createdAt).toLocaleString()}
                            </span>
                          </div>
                          <p
                            className="text-sm truncate"
                            style={{ color: "var(--text-secondary)" }}
                          >
                            {tx.description}
                          </p>
                        </div>

                        {/* Amount */}
                        <div className="text-right flex-shrink-0">
                          <p
                            className="text-lg font-bold"
                            style={{
                              color:
                                tx.amount >= 0
                                  ? "var(--color-success)"
                                  : "var(--color-error)",
                            }}
                          >
                            {tx.amount >= 0 ? "+" : ""}$
                            {Math.abs(tx.amount).toFixed(2)}
                          </p>
                          <p
                            className="text-xs"
                            style={{ color: "var(--text-tertiary)" }}
                          >
                            Balance: ${tx.balanceAfter.toFixed(2)}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </Layout>
    </ProtectedRoute>
  );
}
