import React from "react";
import type { Block } from "./types";
import { TransactionCard } from "../transactionRow";
import "./style.css";

interface Props {
  data: Block;
}

export function BlockCard({ data }: Props) {
  const { blockNumber, cachedHash, header, transactions } = data;

  const formattedTimestamp = new Date(header.timestamp).toLocaleString();

  return (
    <div className="block-card border rounded-lg shadow p-4 bg-white dark:bg-gray-800">
      <header className="block-card-header mb-4">
        <h2 className="text-xl font-bold">Block #{blockNumber}</h2>
        <p className="text-sm">Hash: {cachedHash}</p>
      </header>

      <div className="block-card-body mb-4">
        <h3 className="font-semibold mb-2">Header</h3>
        <p><strong>Previous Hash:</strong> {header.previousHash}</p>
        <p><strong>Merkle Root:</strong> {header.merkleRoot}</p>
        <p><strong>Timestamp:</strong> {formattedTimestamp}</p>
        <p><strong>Nonce:</strong> {header.nonce}</p>
        <p><strong>Difficulty:</strong> {header.difficulty}</p>
      </div>

      {transactions && transactions.length > 0 && (
        <div className="block-transactions">
          <h3 className="font-semibold mb-2">Transactions</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {transactions.map((tx) => (
              <TransactionCard key={tx.txId} tx={tx} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}