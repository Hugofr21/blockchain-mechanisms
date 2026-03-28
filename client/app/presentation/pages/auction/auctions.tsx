import React, { useState } from "react";
import { AuctionCard } from "../../components/auction";
import { CreateAuctionForm } from "../../components/auction/form";
import type { AuctionRow } from "../../components/auction/types";

interface Props {
  initialAuctions: AuctionRow[];
}

export function AuctionsPage({ initialAuctions }: Props) {
  const [auctions, setAuctions] = useState<AuctionRow[]>(initialAuctions);

  const handleCreateAuction = (data: { description: string; startingPrice: string }) => {
    const newAuction: AuctionRow = {
      auctionId: crypto.randomUUID(),
      ownerId: "1001",
      minPrice: data.startingPrice,
      endTimestamp: Date.now() + 3600000,
      bidHistory: [],
      currentHighestBid: data.startingPrice,
      currentWinnerId: "N/A",
      isOpen: true,
    };

    setAuctions((prev) => [newAuction, ...prev]);
  };

  return (
    <main className="max-w-7xl mx-auto p-6 space-y-10">
      <header className="space-y-2">
        <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">
          Auctions
        </h1>
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Create auctions and monitor bids in the distributed ledger.
        </p>
      </header>

      <section className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-6">
        <CreateAuctionForm onSubmit={handleCreateAuction} />
      </section>

      <section className="space-y-4">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          Active Auctions
        </h2>

        {auctions.length === 0 ? (
          <p className="text-gray-500 dark:text-gray-400 text-sm">
            No auctions created yet.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {auctions.map((auction) => (
              <AuctionCard key={auction.auctionId} data={auction} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}