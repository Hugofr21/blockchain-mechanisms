import type { NodeAction } from "../components/nodeAction/types";

export const testActions: NodeAction[] = [
  { id: "action-1", label: "View Blockchain" },
  { id: "action-2", label: "View Auction" },
  { id: "action-3", label: "Attack Sybil" },
  { id: "action-4", label: "Attack Eclipse" },
  { id: "action-5", label: "Attack Replace Bid Duplicate" },
  { id: "action-6", label: "Attack Send Block Validate PoW" },
  { id: "action-7", label: "Shutdown This Node" },
];