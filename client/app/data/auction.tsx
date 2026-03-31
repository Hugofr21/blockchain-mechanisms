import type { AuctionRow } from "../application/model/auction";

export const auctions: AuctionRow[] = [
  {
    auctionId: "auction-001",
    ownerId: "1001",
    minPrice: "50.00",
    endTimestamp: Date.now() + 3600000,
    bidHistory: [
      { auctionId: "auction-001", bidPrice: "55.00", throwTimestamp: Date.now(), newBidderId: "2001" },
    ],
    currentHighestBid: "55.00",
    currentWinnerId: "2001",
    open: true,
  },
  {
    auctionId: "auction-002",
    ownerId: "1002",
    minPrice: "100.00",
    endTimestamp: Date.now() + 7200000,
    bidHistory: [
      { auctionId: "auction-002", bidPrice: "110.00", throwTimestamp: Date.now(), newBidderId: "2002" },
      { auctionId: "auction-002", bidPrice: "120.00", throwTimestamp: Date.now(), newBidderId: "2003" },
    ],
    currentHighestBid: "120.00",
    currentWinnerId: "2003",
    open: true,
  },
  {
    auctionId: "auction-003",
    ownerId: "1003",
    minPrice: "25.00",
    endTimestamp: Date.now() + 1800000,
    bidHistory: [
      { auctionId: "auction-003", bidPrice: "30.00", throwTimestamp: Date.now(), newBidderId: "2004" },
    ],
    currentHighestBid: "30.00",
    currentWinnerId: "2004",
    open: false, 
  },
  {
    auctionId: "auction-004",
    ownerId: "1004",
    minPrice: "75.00",
    endTimestamp: Date.now() + 5400000,
    bidHistory: [],
    currentHighestBid: "0.00",
    currentWinnerId: "0",
    open: true, 
  },
];