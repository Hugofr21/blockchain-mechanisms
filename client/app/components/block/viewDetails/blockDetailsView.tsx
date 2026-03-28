import React from "react";
import type { Block } from "../types";
import { TransactionCard } from "../../transactionRow";

interface Props {
  block: Block;
}

export function BlockDetailsView({ block }: Props) {
  const formattedTimestamp = new Date(block.header.timestamp).toLocaleString();

  return (
    <div className="space-y-6">

      {/* Block Info */}
      <section className="border rounded-2xl shadow-sm bg-white dark:bg-gray-900 p-6 space-y-3">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
          Block #{block.blockNumber}
        </h1>

        <p className="text-sm text-gray-600 dark:text-gray-400">
          Hash: <span className="font-mono">{block.cachedHash}</span>
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
          <p>
            <strong>Previous Hash:</strong>{" "}
            <span className="font-mono">{block.header.previousHash}</span>
          </p>

          <p>
            <strong>Merkle Root:</strong>{" "}
            <span className="font-mono">{block.header.merkleRoot}</span>
          </p>

          <p>
            <strong>Timestamp:</strong> {formattedTimestamp}
          </p>

          <p>
            <strong>Nonce:</strong> {block.header.nonce}
          </p>

          <p>
            <strong>Difficulty:</strong> {block.header.difficulty}
          </p>

          <p>
            <strong>Transactions:</strong> {block.transactions.length}
          </p>
        </div>
      </section>

      {/* Transactions */}
      <section className="border rounded-2xl shadow-sm bg-white dark:bg-gray-900 p-6 space-y-4">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          Transactions
        </h2>

        {block.transactions.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            No transactions in this block.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {block.transactions.map((tx) => (
              <TransactionCard key={tx.txId} tx={tx} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}