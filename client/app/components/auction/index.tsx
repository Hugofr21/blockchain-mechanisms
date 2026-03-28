import React from "react";
import { useNavigate } from "react-router";
import type { AuctionRow } from "./types";

interface Props {
  data: AuctionRow;
}

export const AuctionCard: React.FC<Props> = ({ data }) => {
  const navigate = useNavigate();

  const formattedEndTime = new Date(data.endTimestamp).toLocaleString();

  const handleClick = () => {
    navigate(`/auctions/${data.auctionId}/bids`);
  };

  return (
    <div
      onClick={handleClick}
      className="border rounded-xl p-4 bg-white dark:bg-gray-900 shadow-sm cursor-pointer hover:shadow-md hover:border-indigo-400 transition space-y-2"
    >
      <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
        Auction: {data.auctionId}
      </h3>

      <p className="text-sm text-gray-700 dark:text-gray-300">
        <strong>Owner ID:</strong> {data.ownerId}
      </p>

      <p className="text-sm text-gray-700 dark:text-gray-300">
        <strong>Min Price:</strong> {data.minPrice}
      </p>

      <p className="text-sm text-gray-700 dark:text-gray-300">
        <strong>End Timestamp:</strong> {formattedEndTime}
      </p>

      <p className="text-sm">
        <strong>Status:</strong>{" "}
        {data.isOpen ? (
          <span className="text-green-600 font-semibold">OPEN</span>
        ) : (
          <span className="text-red-600 font-semibold">CLOSED</span>
        )}
      </p>

      <p className="text-sm text-gray-700 dark:text-gray-300">
        <strong>Highest Bid:</strong> {data.currentHighestBid}
      </p>

      <p className="text-xs text-indigo-600 dark:text-indigo-300 font-semibold pt-2">
        Click to open details +
      </p>
    </div>
  );
};