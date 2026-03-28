import React from "react";
import type { TransactionsRow } from "../../../application/model/transaction";
import { TransactionCard } from "../transactionRow";

interface Props {
  transactions: TransactionsRow[];
}

export const TransactionList: React.FC<Props> = ({ transactions }) => {
  if (transactions.length === 0) {
    return <p className="text-gray-500">No transactions found.</p>;
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {transactions.map((tx) => (
        <TransactionCard key={tx.txId} tx={tx} />
      ))}
    </div>
  );
};