"use client";

import Link from "next/link";

export default function Home() {
  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ backgroundColor: "var(--bg-base)" }}
    >
      {/* Hero Section */}
      <section className="flex-1 flex flex-col items-center justify-center px-4 py-16 sm:py-24">
        <div className="max-w-4xl w-full text-center space-y-8">
          {/* Logo & Title */}
          <div className="space-y-4">
            <div className="flex justify-center">
              <div
                className="text-6xl sm:text-7xl animate-bounce"
                style={{ color: "var(--color-primary)" }}
              >
                🎰
              </div>
            </div>
            <h1
              className="text-4xl sm:text-5xl md:text-6xl font-bold"
              style={{ color: "var(--text-primary)" }}
            >
              Casino Platform
            </h1>
            <p
              className="text-lg sm:text-xl"
              style={{ color: "var(--text-secondary)" }}
            >
              Experience premium gaming entertainment
            </p>
          </div>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            <Link
              href="/register"
              className="w-full sm:w-auto px-8 py-4 rounded-lg font-semibold text-lg transition-all shadow-lg hover:shadow-xl transform hover:scale-105"
              style={{
                backgroundColor: "var(--button-primary-bg)",
                color: "var(--button-primary-text)",
                transition: "var(--transition-base)",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor =
                  "var(--button-primary-bg-hover)";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor =
                  "var(--button-primary-bg)";
              }}
            >
              Get Started
            </Link>
            <Link
              href="/login"
              className="w-full sm:w-auto px-8 py-4 rounded-lg font-semibold text-lg transition-all"
              style={{
                backgroundColor: "var(--button-secondary-bg)",
                color: "var(--button-secondary-text)",
                border: "1px solid var(--button-secondary-border)",
                transition: "var(--transition-base)",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor =
                  "var(--button-secondary-bg-hover)";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor =
                  "var(--button-secondary-bg)";
              }}
            >
              Sign In
            </Link>
          </div>

          {/* Features Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-12 max-w-3xl mx-auto">
            {[
              {
                icon: "🔐",
                title: "Secure Authentication",
                description: "Protected accounts with encrypted credentials",
              },
              {
                icon: "💰",
                title: "Virtual Wallet",
                description: "Track your balance and transaction history",
              },
              {
                icon: "🎮",
                title: "Game Lobby",
                description: "Browse games by category and provider",
              },
              {
                icon: "🎯",
                title: "Demo Games",
                description: "Play with virtual money, no risk",
              },
            ].map((feature, index) => (
              <div
                key={index}
                className="p-6 rounded-lg transition-all hover:shadow-lg"
                style={{
                  backgroundColor: "var(--card-bg)",
                  border: "1px solid var(--card-border)",
                  transition: "var(--transition-base)",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = "var(--card-bg-hover)";
                  e.currentTarget.style.boxShadow = "var(--shadow-lg)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "var(--card-bg)";
                  e.currentTarget.style.boxShadow = "none";
                }}
              >
                <div className="text-4xl mb-3">{feature.icon}</div>
                <h3
                  className="text-lg font-semibold mb-2"
                  style={{ color: "var(--text-primary)" }}
                >
                  {feature.title}
                </h3>
                <p
                  className="text-sm"
                  style={{ color: "var(--text-secondary)" }}
                >
                  {feature.description}
                </p>
              </div>
            ))}
          </div>

          {/* Disclaimer */}
          <div
            className="mt-8 p-4 rounded-lg text-xs"
            style={{
              backgroundColor: "var(--bg-elevated)",
              color: "var(--text-tertiary)",
              border: "1px solid var(--border-subtle)",
            }}
          >
            This is a prototype platform using virtual currency only. No real
            money transactions.
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer
        className="py-6 text-center text-sm"
        style={{
          color: "var(--text-tertiary)",
          borderTop: "1px solid var(--border-subtle)",
        }}
      >
        <p>Casino Platform Prototype © 2026</p>
      </footer>
    </div>
  );
}
