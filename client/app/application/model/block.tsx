import type { TransactionsRow } from "./transaction";

export interface BlockHeader {
  version: number;
  previousBlockHash: string; 
  merkleRoot: string;
  timestamp: number;
  difficulty: number;
  nonce: number;
  payloadForMining: string; 
}

export interface Block {
  numberBlock: number; 
  currentBlockHash: string; 
  header: BlockHeader;
  transactions: TransactionsRow[];
}