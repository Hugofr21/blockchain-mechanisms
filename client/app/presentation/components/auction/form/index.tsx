import React, { useState } from "react";

interface CreateAuctionPayload {
  description: string;
  startingPrice: string;
}

interface Props {
  onSubmit?: (data: CreateAuctionPayload) => void;
}

export function CreateAuctionForm({ onSubmit }: Props) {
  const [description, setDescription] = useState("");
  const [startingPrice, setStartingPrice] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!description.trim()) {
      setError("Item Description is required.");
      return;
    }

    if (!startingPrice.trim()) {
      setError("Starting Price is required.");
      return;
    }

    const priceValue = Number(startingPrice);

    if (isNaN(priceValue) || priceValue <= 0) {
      setError("Starting Price must be a valid number greater than 0.");
      return;
    }

    const payload: CreateAuctionPayload = {
      description: description.trim(),
      startingPrice: startingPrice.trim(),
    };

    if (onSubmit) {
      onSubmit(payload);
    }

    setDescription("");
    setStartingPrice("");
  };

  return (
    <div className="border rounded-2xl shadow-sm bg-white dark:bg-gray-900 p-6 space-y-4">
      <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
        Create Auction
      </h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-1">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Item Description
          </label>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Ex: Laptop Dell XPS 13"
            className="w-full border border-gray-300 dark:border-gray-700 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-950 text-gray-900 dark:text-white focus:outline-none focus:ring focus:ring-indigo-300"
          />
        </div>

        <div className="space-y-1">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Starting Price
          </label>
          <input
            type="number"
            step="0.01"
            value={startingPrice}
            onChange={(e) => setStartingPrice(e.target.value)}
            placeholder="Ex: 50.00"
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
          Create Auction
        </button>
      </form>
    </div>
  );
}