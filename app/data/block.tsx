import type { Block } from "../components/block/types";

export const blocks: Block[] = [
  {
    blockNumber: 1,
    cachedHash: "0000abc123",
    header: { 
      previousHash: "0000000000", 
      merkleRoot: "merkle123", 
      timestamp: Date.now(), 
      nonce: 9999, 
      difficulty: 4 
    },
    transactions: [
      {
        txId: "tx-001",
        type: "TRANSFER",
        sender: "user-100",
        ownerId: "user-200",
        data: { amount: 50 },
        timestamp: Date.now(),
        nonce: 1,
      },
      {
        txId: "tx-002",
        type: "AUCTION",
        sender: "user-101",
        ownerId: "auction-001",
        data: { bidPrice: 55 },
        timestamp: Date.now(),
        nonce: 2,
      },
    ],
  },
  {
    blockNumber: 2,
    cachedHash: "0000def456",
    header: { 
      previousHash: "0000abc123", 
      merkleRoot: "merkle456", 
      timestamp: Date.now(), 
      nonce: 10001, 
      difficulty: 4 
    },
    transactions: [
      {
        txId: "tx-003",
        type: "STAKE",
        sender: "user-102",
        ownerId: "stake-001",
        data: { amount: 100 },
        timestamp: Date.now(),
        nonce: 1,
      },
    ],
  },
  {
    blockNumber: 3,
    cachedHash: "0000ghi789",
    header: { 
      previousHash: "0000def456", 
      merkleRoot: "merkle789", 
      timestamp: Date.now(), 
      nonce: 10002, 
      difficulty: 4 
    },
    transactions: [
      {
        txId: "tx-004",
        type: "TRANSFER",
        sender: "user-103",
        ownerId: "user-104",
        data: { amount: 75 },
        timestamp: Date.now(),
        nonce: 1,
      },
      {
        txId: "tx-005",
        type: "AUCTION",
        sender: "user-105",
        ownerId: "auction-002",
        data: { bidPrice: 60 },
        timestamp: Date.now(),
        nonce: 2,
      },
    ],
  },
];