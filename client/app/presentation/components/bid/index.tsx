import React from "react";
import type { BidRow } from "../../../application/model/bid";
import { Gavel, DollarSign, User, Clock, Hash } from "lucide-react";

interface Props {
  data: BidRow;
}

export const BidRowCard: React.FC<Props> = ({ data }) => {
  const formattedTime = new Date(data.throwTimestamp).toLocaleString();

  return (
    <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Bid
          </p>

          <h4 className="text-sm font-semibold text-gray-900 dark:text-white truncate">
            Auction {data.auctionId}
          </h4>
        </div>

        <div className="text-indigo-500">
          <Gavel size={18} />
        </div>
      </div>

      {/* Body */}
      <div className="p-4 space-y-3">
        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <Hash size={14} className="opacity-70" />
          <span className="break-all">
            <span className="font-medium">Auction ID:</span>{" "}
            <span className="font-mono">{data.auctionId}</span>
          </span>
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <DollarSign size={14} className="opacity-70" />
          <span>
            <span className="font-medium">Bid Price:</span>{" "}
            <span className="font-mono font-semibold text-indigo-600 dark:text-indigo-300">
              {data.bidPrice}
            </span>
          </span>
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <User size={14} className="opacity-70" />
          <span className="break-all">
            <span className="font-medium">Bidder:</span>{" "}
            <span className="font-mono">{data.newBidderId}</span>
          </span>
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <Clock size={14} className="opacity-70" />
          <span>
            <span className="font-medium">Timestamp:</span> {formattedTime}
          </span>
        </div>
      </div>
    </div>
  );
};