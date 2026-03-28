
export type TransactionType = "TRANSFER" | "AUCTION" | "STAKE"; // Exemplo de tipos

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