import React, { useState } from "react";

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

    if (onSubmit) {
      onSubmit(auctionId, bidValue);
    }

    setBidValue("");
  };

  return (
    <div className="border rounded-2xl shadow-sm bg-white dark:bg-gray-900 p-6 space-y-4">
      <h2 className="text-xl font-bold text-gray-900 dark:text-white">
        Place Bid
      </h2>

      <p className="text-sm text-gray-600 dark:text-gray-300">
        Auction ID: <span className="font-mono">{auctionId}</span>
      </p>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-1">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Bid Value
          </label>
          <input
            type="number"
            step="0.01"
            value={bidValue}
            onChange={(e) => setBidValue(e.target.value)}
            placeholder="Ex: 55.00"
            className="w-full border border-gray-300 dark:border-gray-700 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-950 text-gray-900 dark:text-white focus:outline-none focus:ring focus:ring-indigo-300"
          />
        </div>

        {error && (
          <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
        )}

        <button
          type="submit"
          className="w-full rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 transition"
        >
          Submit Bid
        </button>
      </form>
    </div>
  );
}