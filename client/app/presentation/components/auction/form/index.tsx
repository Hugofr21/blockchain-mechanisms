import React, { useState } from "react";
import { Gavel, FileText, DollarSign, AlertTriangle, PlusCircle } from "lucide-react";

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

    onSubmit?.(payload);

    setDescription("");
    setStartingPrice("");
  };

  return (
    <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="p-5 border-b border-gray-100 dark:border-gray-800">
        <div className="flex items-center gap-2">
          <Gavel size={18} className="text-indigo-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            Create Auction
          </h2>
        </div>

        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          Publish a new auction into the network.
        </p>
      </div>

      {/* Body */}
      <form onSubmit={handleSubmit} className="p-5 space-y-4">
        <div className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Item Description
          </label>

          <div className="flex items-center gap-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 px-3 py-2">
            <FileText size={16} className="opacity-70" />
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ex: Laptop Dell XPS 13"
              className="w-full bg-transparent outline-none text-sm text-gray-900 dark:text-white"
            />
          </div>
        </div>

        <div className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Starting Price
          </label>

          <div className="flex items-center gap-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-950 px-3 py-2">
            <DollarSign size={16} className="opacity-70" />
            <input
              type="number"
              step="0.01"
              value={startingPrice}
              onChange={(e) => setStartingPrice(e.target.value)}
              placeholder="Ex: 50.00"
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
          <PlusCircle size={18} />
          Create Auction
        </button>
      </form>
    </div>
  );
}