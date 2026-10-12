import Link from "next/link";

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-zinc-50 dark:bg-zinc-900 p-8">
      <main className="max-w-2xl w-full space-y-8">
        <div className="text-center space-y-4">
          <h1 className="text-4xl font-bold text-zinc-900 dark:text-zinc-50">
            Casino Platform
          </h1>
          <p className="text-lg text-zinc-600 dark:text-zinc-400">
            Welcome to the Casino Platform Prototype
          </p>
        </div>

        <div className="bg-white dark:bg-zinc-800 rounded-lg shadow-lg p-8 space-y-6">
          <div className="space-y-4">
            <h2 className="text-2xl font-semibold text-zinc-900 dark:text-zinc-50">
              Get Started
            </h2>
            <p className="text-zinc-600 dark:text-zinc-400">
              Create an account or sign in to access the platform.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Link
              href="/register"
              className="flex items-center justify-center bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-6 rounded-lg transition-colors"
            >
              Sign Up
            </Link>
            <Link
              href="/login"
              className="flex items-center justify-center bg-zinc-600 hover:bg-zinc-700 text-white font-medium py-3 px-6 rounded-lg transition-colors"
            >
              Sign In
            </Link>
          </div>
        </div>

        <div className="bg-zinc-100 dark:bg-zinc-800 rounded-lg p-6 space-y-3">
          <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-50">
            Features
          </h3>
          <ul className="space-y-2 text-sm text-zinc-700 dark:text-zinc-300">
            <li className="flex items-center gap-2">
              <span className="w-2 h-2 bg-green-500 rounded-full"></span>
              <span>✓ User registration and authentication</span>
            </li>
            <li className="flex items-center gap-2">
              <span className="w-2 h-2 bg-green-500 rounded-full"></span>
              <span>✓ Virtual wallet with transaction history</span>
            </li>
            <li className="flex items-center gap-2">
              <span className="w-2 h-2 bg-green-500 rounded-full"></span>
              <span>✓ Game lobby with category filters</span>
            </li>
            <li className="flex items-center gap-2">
              <span className="w-2 h-2 bg-green-500 rounded-full"></span>
              <span>✓ Demo slot game with 25 paylines</span>
            </li>
          </ul>
        </div>
      </main>
    </div>
  );
}
