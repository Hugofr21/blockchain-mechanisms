export type TransactionType = "REGULAR_TRANSFER" | "AUCTION" | "BID";

export interface TransactionsRow {
  txId: string;
  type: string; 
  senderId: string | number; 
  data: any; 
  timestamp: number; 
  signatureBase64?: string; 
  dataSign?: string; 
  nonce: number;
}