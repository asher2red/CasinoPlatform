"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Layout from "@/components/Layout";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/components/Toast";

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  status: string;
  balance: number | null;
  createdAt: string;
}

export default function AdminUsers() {
  const router = useRouter();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (user && user.role !== "ADMIN") {
      router.push("/dashboard");
    } else if (user) {
      fetchUsers();
    }
  }, [user, router, page]);

  const fetchUsers = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/admin/users?page=${page}&size=20`,
        {
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error("Failed to fetch users");
      }

      const data = await response.json();
      setUsers(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      showToast("Failed to load users", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (userId: number, newStatus: string) => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/admin/users/${userId}/status`,
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
        throw new Error("Failed to update user status");
      }

      showToast("User status updated successfully", "success");
      fetchUsers();
    } catch (err) {
      showToast("Failed to update status", "error");
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
            User Management
          </h1>

          <div className="bg-white dark:bg-zinc-800 rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-zinc-200 dark:divide-zinc-700">
              <thead className="bg-zinc-50 dark:bg-zinc-900">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Username
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Email
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Role
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-zinc-500 dark:text-zinc-400 uppercase tracking-wider">
                    Balance
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
                {users.map((u) => (
                  <tr key={u.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      {u.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      {u.username}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-600 dark:text-zinc-400">
                      {u.email}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded ${
                          u.role === "ADMIN"
                            ? "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200"
                            : "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                        }`}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-zinc-900 dark:text-zinc-50">
                      ${u.balance?.toFixed(2) || "0.00"}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded ${
                          u.status === "ACTIVE"
                            ? "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                            : u.status === "SUSPENDED"
                            ? "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
                            : "bg-zinc-100 text-zinc-800 dark:bg-zinc-700 dark:text-zinc-200"
                        }`}
                      >
                        {u.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      {u.status === "ACTIVE" ? (
                        <button
                          onClick={() => handleStatusChange(u.id, "SUSPENDED")}
                          className="text-red-600 hover:text-red-800 font-medium"
                        >
                          Suspend
                        </button>
                      ) : (
                        <button
                          onClick={() => handleStatusChange(u.id, "ACTIVE")}
                          className="text-green-600 hover:text-green-800 font-medium"
                        >
                          Activate
                        </button>
                      )}
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
