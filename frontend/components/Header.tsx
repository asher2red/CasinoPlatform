"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

interface WalletBalance {
  balance: string;
  currency: string;
}

export default function Header() {
  const router = useRouter();
  const pathname = usePathname();
  const { user } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [balance, setBalance] = useState<WalletBalance | null>(null);
  const [userMenuOpen, setUserMenuOpen] = useState(false);

  useEffect(() => {
    if (user) {
      fetchBalance();
    }
  }, [user]);

  const fetchBalance = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/wallet/balance", {
        credentials: "include",
      });
      if (response.ok) {
        const data = await response.json();
        setBalance(data);
      }
    } catch (err) {
      console.error("Failed to fetch balance:", err);
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

  const navLinks = [
    { href: "/dashboard", label: "Dashboard" },
    { href: "/games", label: "Games" },
    { href: "/wallet", label: "Wallet" },
  ];

  const isActive = (href: string) => pathname === href;

  if (!user) return null;

  return (
    <header
      className="sticky top-0 z-50 border-b"
      style={{
        backgroundColor: "var(--nav-bg)",
        borderColor: "var(--nav-border)",
        height: "var(--nav-height)",
      }}
    >
      <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-full">
        <div className="flex items-center justify-between h-full">
          {/* Logo */}
          <Link
            href="/dashboard"
            className="flex items-center gap-2 transition-opacity hover:opacity-80"
            style={{ transition: "var(--transition-fast)" }}
          >
            <div
              className="text-2xl font-bold"
              style={{ color: "var(--color-primary)" }}
            >
              🎰
            </div>
            <span
              className="text-xl font-bold hidden sm:inline"
              style={{ color: "var(--text-primary)" }}
            >
              Casino Platform
            </span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-8">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="text-sm font-medium transition-colors"
                style={{
                  color: isActive(link.href)
                    ? "var(--nav-link-active)"
                    : "var(--nav-link-color)",
                  transition: "var(--transition-fast)",
                }}
                onMouseEnter={(e) => {
                  if (!isActive(link.href)) {
                    e.currentTarget.style.color = "var(--nav-link-hover)";
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive(link.href)) {
                    e.currentTarget.style.color = "var(--nav-link-color)";
                  }
                }}
              >
                {link.label}
              </Link>
            ))}
            {user?.role === "ADMIN" && (
              <Link
                href="/admin"
                className="text-sm font-medium"
                style={{
                  color: isActive("/admin")
                    ? "var(--color-purple-500)"
                    : "var(--color-purple-600)",
                  transition: "var(--transition-fast)",
                }}
              >
                ⚙️ Admin
              </Link>
            )}
          </div>

          {/* User Menu */}
          <div className="flex items-center gap-4">
            {/* Balance Display */}
            {balance && (
              <Link
                href="/wallet"
                className="hidden sm:flex items-center gap-2 px-4 py-2 rounded-lg transition-colors"
                style={{
                  backgroundColor: "var(--balance-bg)",
                  transition: "var(--transition-fast)",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = "var(--bg-overlay)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "var(--balance-bg)";
                }}
              >
                <span
                  className="text-xs font-medium"
                  style={{ color: "var(--balance-label)" }}
                >
                  Balance:
                </span>
                <span
                  className="text-sm font-bold"
                  style={{ color: "var(--balance-text)" }}
                >
                  ${parseFloat(balance.balance).toFixed(2)}
                </span>
              </Link>
            )}

            {/* User Avatar & Menu - Desktop */}
            <div className="hidden md:block relative">
              <button
                onClick={() => setUserMenuOpen(!userMenuOpen)}
                className="flex items-center gap-2 px-3 py-2 rounded-lg transition-colors"
                style={{
                  backgroundColor: userMenuOpen
                    ? "var(--bg-elevated)"
                    : "transparent",
                  transition: "var(--transition-fast)",
                }}
                onMouseEnter={(e) => {
                  if (!userMenuOpen) {
                    e.currentTarget.style.backgroundColor =
                      "var(--interactive-hover)";
                  }
                }}
                onMouseLeave={(e) => {
                  if (!userMenuOpen) {
                    e.currentTarget.style.backgroundColor = "transparent";
                  }
                }}
              >
                <div
                  className="w-8 h-8 rounded-full flex items-center justify-center font-medium text-sm"
                  style={{
                    backgroundColor: "var(--color-primary)",
                    color: "var(--text-on-primary)",
                  }}
                >
                  {user.username.charAt(0).toUpperCase()}
                </div>
                <span
                  className="text-sm font-medium"
                  style={{ color: "var(--text-primary)" }}
                >
                  {user.username}
                </span>
                <svg
                  className="w-4 h-4 transition-transform"
                  style={{
                    color: "var(--text-secondary)",
                    transform: userMenuOpen ? "rotate(180deg)" : "rotate(0)",
                    transition: "var(--transition-fast)",
                  }}
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M19 9l-7 7-7-7"
                  />
                </svg>
              </button>

              {/* Dropdown Menu */}
              {userMenuOpen && (
                <div
                  className="absolute right-0 mt-2 w-48 rounded-lg shadow-lg py-2"
                  style={{
                    backgroundColor: "var(--bg-elevated)",
                    border: "1px solid var(--border-subtle)",
                    boxShadow: "var(--shadow-xl)",
                  }}
                >
                  <div className="px-4 py-2 border-b" style={{ borderColor: "var(--border-subtle)" }}>
                    <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
                      Signed in as
                    </p>
                    <p className="text-sm font-medium" style={{ color: "var(--text-primary)" }}>
                      {user.username}
                    </p>
                    <p className="text-xs" style={{ color: "var(--text-tertiary)" }}>
                      {user.email}
                    </p>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-4 py-2 text-sm transition-colors"
                    style={{
                      color: "var(--color-error)",
                      transition: "var(--transition-fast)",
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.backgroundColor =
                        "var(--interactive-hover)";
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.backgroundColor = "transparent";
                    }}
                  >
                    Sign out
                  </button>
                </div>
              )}
            </div>

            {/* Mobile Menu Button */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-2 rounded-lg transition-colors"
              style={{
                color: "var(--text-primary)",
                backgroundColor: mobileMenuOpen
                  ? "var(--bg-elevated)"
                  : "transparent",
                transition: "var(--transition-fast)",
              }}
              onMouseEnter={(e) => {
                if (!mobileMenuOpen) {
                  e.currentTarget.style.backgroundColor =
                    "var(--interactive-hover)";
                }
              }}
              onMouseLeave={(e) => {
                if (!mobileMenuOpen) {
                  e.currentTarget.style.backgroundColor = "transparent";
                }
              }}
            >
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                {mobileMenuOpen ? (
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                ) : (
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M4 6h16M4 12h16M4 18h16"
                  />
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div
            className="md:hidden absolute left-0 right-0 mt-2 mx-4 rounded-lg shadow-lg py-2"
            style={{
              backgroundColor: "var(--bg-elevated)",
              border: "1px solid var(--border-subtle)",
              boxShadow: "var(--shadow-xl)",
            }}
          >
            <div className="px-4 py-3 border-b" style={{ borderColor: "var(--border-subtle)" }}>
              <p className="text-sm font-medium" style={{ color: "var(--text-primary)" }}>
                {user.username}
              </p>
              <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
                {user.email}
              </p>
              {balance && (
                <div className="mt-2 flex items-center gap-2">
                  <span className="text-xs" style={{ color: "var(--text-secondary)" }}>
                    Balance:
                  </span>
                  <span className="text-sm font-bold" style={{ color: "var(--balance-text)" }}>
                    ${parseFloat(balance.balance).toFixed(2)}
                  </span>
                </div>
              )}
            </div>
            <div className="py-2">
              {navLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className="block px-4 py-2 text-sm transition-colors"
                  style={{
                    color: isActive(link.href)
                      ? "var(--nav-link-active)"
                      : "var(--text-primary)",
                    backgroundColor: isActive(link.href)
                      ? "var(--interactive-hover)"
                      : "transparent",
                    transition: "var(--transition-fast)",
                  }}
                  onMouseEnter={(e) => {
                    if (!isActive(link.href)) {
                      e.currentTarget.style.backgroundColor =
                        "var(--interactive-hover)";
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!isActive(link.href)) {
                      e.currentTarget.style.backgroundColor = "transparent";
                    }
                  }}
                >
                  {link.label}
                </Link>
              ))}
              {user?.role === "ADMIN" && (
                <Link
                  href="/admin"
                  onClick={() => setMobileMenuOpen(false)}
                  className="block px-4 py-2 text-sm"
                  style={{
                    color: "var(--color-purple-500)",
                    transition: "var(--transition-fast)",
                  }}
                >
                  ⚙️ Admin
                </Link>
              )}
            </div>
            <div className="border-t pt-2" style={{ borderColor: "var(--border-subtle)" }}>
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  handleLogout();
                }}
                className="w-full text-left px-4 py-2 text-sm transition-colors"
                style={{
                  color: "var(--color-error)",
                  transition: "var(--transition-fast)",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor =
                    "var(--interactive-hover)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "transparent";
                }}
              >
                Sign out
              </button>
            </div>
          </div>
        )}
      </nav>
    </header>
  );
}
