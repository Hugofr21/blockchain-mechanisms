import type { NodeAction } from "../components/nodeAction/types";

export const testActions: NodeAction[] = [
  { id: "action-1", label: "View Blockchain" },
  { id: "action-2", label: "Create Auction" },
  { id: "action-3", label: "Add Bid to Auction" },
  { id: "action-4", label: "View Auction Bids" },
  { id: "action-5", label: "Attack Sybil" },
  { id: "action-6", label: "Attack Eclipse" },
  { id: "action-7", label: "Attack Replace Bid Duplicate" },
  { id: "action-8", label: "Attack Send Block Validate PoW" },
  { id: "action-9", label: "Shutdown This Node" },
];