import type { BidRow } from "./bid";

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