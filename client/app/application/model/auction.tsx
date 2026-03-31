import type { BidRow } from "./bid";

export interface AuctionRow {
  auctionId: string;
  ownerId: string | number; 
  minPrice: string | number; 
  endTimestamp: number;       
  bidHistory: BidRow[];       
  currentHighestBid: string | number;  
  currentWinnerId: string | number | null;    
  open: boolean; 
}