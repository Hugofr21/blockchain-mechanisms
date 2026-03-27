import React from "react";
import type { BidRow } from "./types";

interface Props {
  data: BidRow;
}

export const BidRowCard: React.FC<Props> = ({ data }) => {
  const formattedTime = new Date(data.throwTimestamp).toLocaleString();

  return (
    <div className="bid-row border rounded p-4">
      <h4 className="font-semibold text-lg">Bid</h4>
      <p><strong>Auction ID:</strong> {data.auctionId}</p>
      <p><strong>Bid Price:</strong> {data.bidPrice}</p>
      <p><strong>New Bidder ID:</strong> {data.newBidderId}</p>
      <p><strong>Timestamp:</strong> {formattedTime}</p>
    </div>
  );
};