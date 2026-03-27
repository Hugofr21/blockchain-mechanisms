import type { BidRow } from "../bid/types";

export interface AuctionRow {
  auctionId: string;
  ownerId: string;          
  minPrice: string;           
  endTimestamp: number;       
  bidHistory: BidRow[];       
  currentHighestBid: string;  
  currentWinnerId: string;    
  isOpen: boolean;
}