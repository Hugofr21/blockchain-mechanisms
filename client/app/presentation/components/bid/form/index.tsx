import React, { useState } from "react";
import { Gavel, DollarSign, AlertTriangle, Send, Hash } from "lucide-react";

interface Props {
  auctionId: string;
  onSubmit?: (auctionId: string, bidValue: string) => void;
}

export function PlaceBidForm({ auctionId, onSubmit }: Props) {
  const [bidValue, setBidValue] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!auctionId.trim()) {
      setError("Auction ID is required.");
      return;
    }

    if (!bidValue.trim()) {
      setError("Bid Value is required.");
      return;
    }

    const parsed = Number(bidValue);

    if (isNaN(parsed) || parsed <= 0) {
      setError("Invalid bid format! Must be a number > 0.");
      return;
    }

    onSubmit?.(auctionId, bidValue);

    setBidValue("");
  };

  return (
    <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="p-5 border-b border-gray-100 dark:border-gray-800">
        <div className="flex items-center gap-2">
          <Gavel size={18} className="text-indigo-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            Place Bid
          </h2>
        </div>

        <div className="mt-2 flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400 break-all">
          <Hash size={14} className="opacity-70" />
          <span>
            Auction ID: <span className="font-mono font-semibold">{auctionId}</span>
          </span>
        </div>
      </div>

      {/* Body */}
      <form onSubmit={handleSubmit} className="p-5 space-y-4">
        <div className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Bid Value
          </label>

          <div className="flex items-center gap-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 px-3 py-2">
            <DollarSign size={16} className="opacity-70" />
            <input
              type="number"
              step="0.01"
              value={bidValue}
              onChange={(e) => setBidValue(e.target.value)}
              placeholder="Ex: 55.00"
              className="w-full bg-transparent outline-none text-sm text-gray-900 dark:text-white"
            />
          </div>
        </div>

        {error && (
          <div className="flex items-start gap-2 text-sm text-red-600 dark:text-red-400">
            <AlertTriangle size={16} className="mt-0.5" />
            <p>{error}</p>
          </div>
        )}

        <button
          type="submit"
          className="w-full flex items-center justify-center gap-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 transition"
        >
          <Send size={18} />
          Submit Bid
        </button>
      </form>
    </div>
  );
}