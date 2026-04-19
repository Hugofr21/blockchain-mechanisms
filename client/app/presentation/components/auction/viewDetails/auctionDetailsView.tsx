import React from "react";
import type { AuctionRow } from "../../../../application/model/auction";
import type { BidRow } from "../../../../application/model/bid";
import {
  Gavel,
  User,
  DollarSign,
  Trophy,
  Clock,
  BadgeCheck,
  BadgeX,
  FileText,
} from "lucide-react";

interface Props {
  auction: AuctionRow;
}

export function AuctionDetailsView({ auction }: Props) {
  const formattedEnd = new Date(auction.endTimestamp).toLocaleString();
  const sortedBids = [...(auction.bidHistory || [])].sort(
    (a, b) => b.throwTimestamp - a.throwTimestamp
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Gavel size={22} className="text-indigo-500" />
            <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">
              Auction Details
            </h1>
          </div>

          <p className="text-sm text-gray-600 dark:text-gray-400 break-all">
            Auction ID:{" "}
            <span className="font-mono font-semibold">{auction.auctionId}</span>
          </p>
        </div>

        {auction.open ? (
          <span className="flex items-center gap-1 text-xs font-semibold px-3 py-1 rounded-full border border-green-200 dark:border-green-900 bg-green-50 dark:bg-green-950/30 text-green-700 dark:text-green-300">
            <BadgeCheck size={14} />
            OPEN
          </span>
        ) : (
          <span className="flex items-center gap-1 text-xs font-semibold px-3 py-1 rounded-full border border-red-200 dark:border-red-900 bg-red-50 dark:bg-red-950/30 text-red-700 dark:text-red-300">
            <BadgeX size={14} />
            CLOSED
          </span>
        )}
      </div>

      {/* Metadata */}
      <section className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-gray-100 dark:border-gray-800">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            Auction Metadata
          </h2>
        </div>

      <div className="p-5 grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div className="flex items-start gap-2 min-w-0">
            <User size={16} className="opacity-70 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Owner ID
              </p>
              <p className="font-mono text-gray-800 dark:text-gray-200 break-all whitespace-normal">
                {auction.ownerId}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2 min-w-0">
            <DollarSign size={16} className="opacity-70 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Min Price
              </p>
              <p className="font-mono text-gray-800 dark:text-gray-200">
                {auction.minPrice}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2 min-w-0">
            <Trophy size={16} className="opacity-70 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Current Winner
              </p>
              <p className="font-mono text-gray-800 dark:text-gray-200 break-all whitespace-normal">
                {auction.currentWinnerId || "N/A"}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2 min-w-0">
            <DollarSign size={16} className="opacity-70 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Highest Bid
              </p>
              <p className="font-mono font-semibold text-indigo-600 dark:text-indigo-300">
                {auction.currentHighestBid}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2 min-w-0">
            <Clock size={16} className="opacity-70 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                End Timestamp
              </p>
              <p className="text-gray-800 dark:text-gray-200 break-words whitespace-normal">
                {formattedEnd}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2 min-w-0">
            <FileText size={16} className="opacity-70 mt-0.5 shrink-0" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Bid History
              </p>
              <p className="font-mono text-gray-800 dark:text-gray-200">
                {sortedBids.length} bids
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Bid History */}
      <section className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-gray-100 dark:border-gray-800">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            Bid History
          </h2>
        </div>

        <div className="p-5">
          {sortedBids.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No bids registered for this auction.
            </p>
          ) : (
            <div className="space-y-3">
              {sortedBids.map((bid: BidRow) => (
                <div
                  key={`${bid.auctionId}-${bid.newBidderId}-${bid.throwTimestamp}-${bid.bidPrice}`}
                  className="rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-950 p-4"
                >
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-gray-600 dark:text-gray-400">
                      Bidder
                    </p>

                    <span className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-600 dark:text-gray-300">
                      {new Date(bid.throwTimestamp).toLocaleString()}
                    </span>
                  </div>

                  <p className="mt-1 text-sm font-mono text-gray-800 dark:text-gray-200 break-all">
                    {bid.newBidderId}
                  </p>

                  <p className="mt-3 text-sm text-gray-700 dark:text-gray-300">
                    <span className="font-medium">Bid Price:</span>{" "}
                    <span className="font-mono font-semibold text-indigo-600 dark:text-indigo-300">
                      {bid.bidPrice}
                    </span>
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}