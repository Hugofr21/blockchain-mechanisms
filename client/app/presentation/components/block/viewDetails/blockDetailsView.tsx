import React from "react";
import type { Block } from "../../../../application/model/block";
import { TransactionCard } from "../../transactionRow";
import {
  Blocks,
  Hash,
  Clock,
  Shield,
  Layers,
  FileText,
  ArrowLeftRight,
} from "lucide-react";

interface Props {
  block: Block;
}

export function BlockDetailsView({ block }: Props) {
  const formattedTimestamp = new Date(block.header.timestamp).toLocaleString();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Blocks size={22} className="text-indigo-500" />
            <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">
              Block #{block.numberBlock}
            </h1>
          </div>

          <p className="text-sm text-gray-600 dark:text-gray-400">
            Visualização detalhada do bloco e suas transações.
          </p>
        </div>

        <div className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-600 dark:text-gray-300">
          TX: <span className="font-semibold">{block.transactions.length}</span>
        </div>
      </div>

      {/* Block Info */}
      <section className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-gray-100 dark:border-gray-800">
          <div className="flex items-center gap-2">
            <Hash size={18} className="text-indigo-500" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Block Metadata
            </h2>
          </div>

          <p className="mt-2 text-xs text-gray-600 dark:text-gray-400 break-all">
            <span className="font-medium">Current Hash:</span>{" "}
            <span className="font-mono">{block.currentBlockHash}</span>
          </p>
        </div>

        <div className="p-5 grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div className="flex items-start gap-2">
            <ArrowLeftRight size={16} className="opacity-70 mt-0.5" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Previous Hash
              </p>
              <p className="font-mono text-xs break-all text-gray-800 dark:text-gray-200">
                {block.header.previousBlockHash}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Layers size={16} className="opacity-70 mt-0.5" />
            <div className="min-w-0">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Merkle Root
              </p>
              <p className="font-mono text-xs break-all text-gray-800 dark:text-gray-200">
                {block.header.merkleRoot}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Clock size={16} className="opacity-70 mt-0.5" />
            <div>
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Timestamp
              </p>
              <p className="text-sm text-gray-800 dark:text-gray-200">
                {formattedTimestamp}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <FileText size={16} className="opacity-70 mt-0.5" />
            <div>
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Nonce
              </p>
              <p className="font-mono text-sm text-gray-800 dark:text-gray-200">
                {block.header.nonce}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Shield size={16} className="opacity-70 mt-0.5" />
            <div>
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-500">
                Difficulty
              </p>
              <p className="font-mono text-sm text-gray-800 dark:text-gray-200">
                {block.header.difficulty}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Blocks size={16} className="opacity-70 mt-0.5" />
            <div>
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                Transactions
              </p>
              <p className="font-mono text-sm text-gray-800 dark:text-gray-200">
                {block.transactions.length}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Transactions */}
      <section className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FileText size={18} className="text-indigo-500" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Transactions
            </h2>
          </div>

          <span className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-600 dark:text-gray-300">
            Total:{" "}
            <span className="font-semibold">{block.transactions.length}</span>
          </span>
        </div>

        <div className="p-5">
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
        </div>
      </section>
    </div>
  );
}