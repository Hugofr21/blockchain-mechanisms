import React, { useState } from "react";
import { useParams, Link } from "react-router";
import { auctions as initialAuctions } from "~/data/auction";
import { AuctionDetailsView } from "~/presentation/components/auction/viewDetails/auctionDetailsView";
import { PlaceBidForm } from "~/presentation/components/bid/form";
import type { AuctionRow } from "~/presentation/components/auction/types";
import type { BidRow } from "~/presentation/components/bid/types";

export default function AuctionBidsPage() {
  const { id } = useParams();

  const [auctions, setAuctions] = useState<AuctionRow[]>(initialAuctions);

  const auction = auctions.find((a) => a.auctionId === id);

  const handlePlaceBid = (auctionId: string, bidValue: string) => {
    const newBid: BidRow = {
      auctionId,
      bidPrice: bidValue,
      throwTimestamp: Date.now(),
      newBidderId: "2001", 
    };

    setAuctions((prev) =>
      prev.map((a) => {
        if (a.auctionId !== auctionId) return a;

        const updatedHistory = [...a.bidHistory, newBid];

        return {
          ...a,
          bidHistory: updatedHistory,
          currentHighestBid: bidValue,
          currentWinnerId: newBid.newBidderId,
        };
      })
    );
  };

  if (!auction) {
    return (
      <main className="max-w-7xl mx-auto p-6 space-y-4">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Auction not found
        </h1>

        <Link
          to="/auctions"
          className="text-indigo-600 hover:underline text-sm"
        >
          Back to Auctions
        </Link>
      </main>
    );
  }

  return (
    <main className="max-w-7xl mx-auto p-6 space-y-8">
      <header className="flex items-center justify-between">
        <h1 className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white">
          Auction Explorer
        </h1>

        <Link
          to="/auctions"
          className="px-4 py-2 rounded-lg bg-gray-100 dark:bg-gray-800 text-sm font-semibold hover:bg-gray-200 dark:hover:bg-gray-700 transition"
        >
          Back
        </Link>
      </header>

      {/* Auction Details */}
      <AuctionDetailsView auction={auction} />

      {/* Place Bid */}
      {auction.isOpen ? (
        <PlaceBidForm auctionId={auction.auctionId} onSubmit={handlePlaceBid} />
      ) : (
        <div className="border rounded-xl p-4 bg-red-50 dark:bg-red-950 text-red-700 dark:text-red-300">
          This auction is CLOSED. No more bids allowed.
        </div>
      )}
    </main>
  );
}