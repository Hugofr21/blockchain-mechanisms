
export type TransactionType = "TRANSFER" | "AUCTION" | "BID";

export interface TransactionsRow {
  txId: string;
  type: TransactionType;
  sender: string; 
  ownerId: string;
  data: any; 
  timestamp: number; 
  signature?: string; 
  nonce: number;
}