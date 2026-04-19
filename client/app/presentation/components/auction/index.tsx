import React from "react";
import { useNavigate } from "react-router";
import type { AuctionRow } from "../../../application/model/auction";
import { Gavel, User, Clock, DollarSign, BadgeCheck, BadgeX, ChevronRight } from "lucide-react";

interface Props {
  data: AuctionRow;
  targetNodePort: string;
}

export const AuctionCard: React.FC<Props> = ({ data, targetNodePort }) => {
  const navigate = useNavigate();
  const formattedEndTime = new Date(data.endTimestamp).toLocaleString();

  const handleClick = () => {
    navigate(`/node/${targetNodePort}/auctions/${data.auctionId}/bids`);
  };

  return (
    <div
      onClick={handleClick}
      className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm hover:shadow-md transition cursor-pointer overflow-hidden"
    >
      {/* Header */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Auction
          </p>
          <h3
            className="text-sm font-semibold text-gray-900 dark:text-white truncate"
            title={data.auctionId}
          >
            {data.auctionId}
          </h3>
        </div>

        <div className="text-indigo-500">
          <Gavel size={18} />
        </div>
      </div>

      {/* Body */}
      <div className="p-4 space-y-3">
          <div className="flex items-start gap-2 text-xs text-gray-600 dark:text-gray-400 min-w-0">
            <User size={14} className="opacity-70 shrink-0 mt-0.5" />
            <span className="min-w-0">
              <span className="font-medium">Owner:</span>{" "}
              <span className="font-mono break-all whitespace-normal">
                {data.ownerId}
              </span>
            </span>
          </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <DollarSign size={14} className="opacity-70" />
          <span>
            <span className="font-medium">Min Price:</span>{" "}
            <span className="font-mono">{data.minPrice}</span>
          </span>
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <Clock size={14} className="opacity-70" />
          <span className="truncate">
            <span className="font-medium">End:</span> {formattedEndTime}
          </span>
        </div>

        <div className="flex items-center justify-between pt-2">
          <span className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300">
            Highest: <span className="font-semibold">{data.currentHighestBid}</span>
          </span>

          {data.open ? (
            <span className="flex items-center gap-1 text-xs font-semibold text-green-600 dark:text-green-400">
              <BadgeCheck size={14} />
              OPEN
            </span>
          ) : (
            <span className="flex items-center gap-1 text-xs font-semibold text-red-600 dark:text-red-400">
              <BadgeX size={14} />
              CLOSED
            </span>
          )}
        </div>

        <div className="pt-2 flex items-center justify-between text-indigo-600 dark:text-indigo-300 text-xs font-semibold">
          <span>Open details</span>
          <ChevronRight size={16} className="opacity-70" />
        </div>
      </div>
    </div>
  );
};