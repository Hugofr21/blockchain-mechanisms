import React from "react";
import type { TransactionsRow } from "../../../application/model/transaction";
import { TransactionCard } from "../transactionRow";
import { FileText, ServerOff } from "lucide-react";

interface Props {
  transactions: TransactionsRow[];
  title?: string;
}

export const TransactionList: React.FC<Props> = ({
  transactions,
  title = "Transactions",
}) => {
  if (!transactions || transactions.length === 0) {
    return (
      <div className="flex items-center gap-3 p-4 rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm">
        <ServerOff className="text-gray-400" size={18} />
        <p className="text-sm text-gray-600 dark:text-gray-400">
          No transactions found.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <FileText size={18} className="text-indigo-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            {title}
          </h2>
        </div>

        <span className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-600 dark:text-gray-300">
          Total:{" "}
          <span className="font-semibold">{transactions.length}</span>
        </span>
      </div>

      {/* Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        {transactions.map((tx) => (
          <TransactionCard key={tx.txId} tx={tx} />
        ))}
      </div>
    </div>
  );
};