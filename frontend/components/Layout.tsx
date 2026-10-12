import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

interface LayoutProps {
  children: React.ReactNode;
  showNav?: boolean;
}

export default function Layout({ children, showNav = true }: LayoutProps) {
  const router = useRouter();
  const { user } = useAuth();

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

  return (
    <div className="min-h-screen bg-zinc-50 dark:bg-zinc-900">
      {showNav && (
        <nav className="bg-white dark:bg-zinc-800 shadow">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16">
              <div className="flex items-center gap-8">
                <Link
                  href="/dashboard"
                  className="text-xl font-bold text-zinc-900 dark:text-zinc-50 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                >
                  Casino Platform
                </Link>
                <div className="hidden md:flex items-center gap-6">
                  <Link
                    href="/dashboard"
                    className="text-sm font-medium text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50 transition-colors"
                  >
                    Dashboard
                  </Link>
                  <Link
                    href="/games"
                    className="text-sm font-medium text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50 transition-colors"
                  >
                    Games
                  </Link>
                  <Link
                    href="/wallet"
                    className="text-sm font-medium text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50 transition-colors"
                  >
                    Wallet
                  </Link>
                  {user?.role === "ADMIN" && (
                    <Link
                      href="/admin"
                      className="text-sm font-medium text-purple-600 hover:text-purple-800 dark:text-purple-400 dark:hover:text-purple-300 transition-colors"
                    >
                      ⚙️ Admin
                    </Link>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-4">
                <button
                  onClick={handleLogout}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-colors"
                >
                  Logout
                </button>
              </div>
            </div>
          </div>
        </nav>
      )}
      <main>{children}</main>
    </div>
  );
}
