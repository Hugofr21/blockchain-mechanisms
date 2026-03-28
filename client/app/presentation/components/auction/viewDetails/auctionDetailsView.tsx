import React from "react";
import type { AuctionRow } from "../types";
import type { BidRow } from "../../bid/types";

interface Props {
  auction: AuctionRow;
}

export function AuctionDetailsView({ auction }: Props) {
  const formattedEnd = new Date(auction.endTimestamp).toLocaleString();

  return (
    <div className="space-y-6">

      {/* Auction Header */}
      <div className="border rounded-2xl shadow-sm bg-white dark:bg-gray-900 p-6 space-y-2">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
          Auction Details
        </h1>

        <p className="text-sm text-gray-600 dark:text-gray-400">
          Auction ID: <span className="font-mono">{auction.auctionId}</span>
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm mt-4">
          <p><strong>Owner ID:</strong> {auction.ownerId}</p>
          <p><strong>Min Price:</strong> {auction.minPrice}</p>
          <p><strong>Current Highest Bid:</strong> {auction.currentHighestBid}</p>
          <p><strong>Current Winner:</strong> {auction.currentWinnerId}</p>
          <p><strong>Status:</strong> {auction.isOpen ? "OPEN" : "CLOSED"}</p>
          <p><strong>End Timestamp:</strong> {formattedEnd}</p>
        </div>
      </div>

      {/* Bid History */}
      <div className="border rounded-2xl shadow-sm bg-white dark:bg-gray-900 p-6 space-y-4">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          Bid History
        </h2>

        {auction.bidHistory.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            No bids registered for this auction.
          </p>
        ) : (
          <div className="space-y-3">
            {auction.bidHistory.map((bid: BidRow, idx: number) => (
              <div
                key={idx}
                className="border rounded-xl p-4 bg-gray-50 dark:bg-gray-950 text-sm space-y-1"
              >
                <p>
                  <strong>Bidder ID:</strong>{" "}
                  <span className="font-mono">{bid.newBidderId}</span>
                </p>

                <p>
                  <strong>Bid Price:</strong>{" "}
                  <span className="font-semibold text-indigo-600 dark:text-indigo-300">
                    {bid.bidPrice}
                  </span>
                </p>

                <p>
                  <strong>Timestamp:</strong>{" "}
                  {new Date(bid.throwTimestamp).toLocaleString()}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  );
}