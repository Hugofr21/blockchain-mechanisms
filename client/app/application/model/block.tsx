import type { TransactionsRow } from "./transaction";

export interface BlockHeader {
  previousHash: string;
  merkleRoot: string;
  timestamp: number;
  nonce: number;
  difficulty: number;
}

export interface Block {
  blockNumber: number;
  cachedHash: string;
  header: BlockHeader;
  transactions: TransactionsRow[];
}