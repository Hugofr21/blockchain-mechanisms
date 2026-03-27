import React from "react";
import type { AuctionRow } from "./types";
import { BidRowCard } from "../bid/index";

interface Props {
  data: AuctionRow;
}

export const AuctionCard: React.FC<Props> = ({ data }) => {
  const formattedEndTime = new Date(data.endTimestamp).toLocaleString();

  return (
    <div className="auction-card border rounded p-4 space-y-2">
      <h3 className="text-xl font-semibold">
        Auction: {data.auctionId}
      </h3>

      <p><strong>Owner ID:</strong> {data.ownerId}</p>
      <p><strong>Min Price:</strong> {data.minPrice}</p>
      <p><strong>End Timestamp:</strong> {formattedEndTime}</p>

      <p>
        <strong>Status:</strong>{" "}
        {data.isOpen ? "OPEN" : "CLOSED"}
      </p>

      <p><strong>Current Highest Bid:</strong> {data.currentHighestBid}</p>
      <p><strong>Current Winner ID:</strong> {data.currentWinnerId}</p>

      <div className="bid-history mt-4">
        <h4 className="text-lg font-medium">Bid History</h4>

        {data.bidHistory.length === 0 ? (
          <p>No bids yet.</p>
        ) : (
          <div className="space-y-2">
            {data.bidHistory.map((bid, index) => (
              <BidRowCard key={index} data={bid} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};