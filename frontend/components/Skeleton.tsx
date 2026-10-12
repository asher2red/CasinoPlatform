export function CardSkeleton() {
  return (
    <div className="bg-white dark:bg-zinc-800 rounded-lg shadow overflow-hidden animate-pulse">
      <div className="h-48 bg-zinc-200 dark:bg-zinc-700"></div>
      <div className="p-6 space-y-3">
        <div className="h-4 bg-zinc-200 dark:bg-zinc-700 rounded w-3/4"></div>
        <div className="h-3 bg-zinc-200 dark:bg-zinc-700 rounded w-full"></div>
        <div className="h-3 bg-zinc-200 dark:bg-zinc-700 rounded w-5/6"></div>
        <div className="h-10 bg-zinc-200 dark:bg-zinc-700 rounded mt-4"></div>
      </div>
    </div>
  );
}

export function WalletSkeleton() {
  return (
    <div className="bg-white dark:bg-zinc-800 rounded-lg shadow p-6 animate-pulse">
      <div className="h-6 bg-zinc-200 dark:bg-zinc-700 rounded w-32 mb-4"></div>
      <div className="h-12 bg-zinc-200 dark:bg-zinc-700 rounded w-48"></div>
    </div>
  );
}

export function TransactionSkeleton() {
  return (
    <div className="flex justify-between items-center border-b border-zinc-200 dark:border-zinc-700 pb-4 animate-pulse">
      <div className="flex-1 space-y-2">
        <div className="flex items-center gap-2">
          <div className="h-6 w-20 bg-zinc-200 dark:bg-zinc-700 rounded"></div>
          <div className="h-4 w-32 bg-zinc-200 dark:bg-zinc-700 rounded"></div>
        </div>
        <div className="h-4 w-48 bg-zinc-200 dark:bg-zinc-700 rounded"></div>
      </div>
      <div className="text-right space-y-2">
        <div className="h-6 w-24 bg-zinc-200 dark:bg-zinc-700 rounded ml-auto"></div>
        <div className="h-3 w-20 bg-zinc-200 dark:bg-zinc-700 rounded ml-auto"></div>
      </div>
    </div>
  );
}

export function ListSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="space-y-4">
      {Array.from({ length: count }).map((_, i) => (
        <TransactionSkeleton key={i} />
      ))}
    </div>
  );
}
