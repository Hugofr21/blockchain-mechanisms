import React from "react";
import type { Block } from "../../components/block/types";
import { TransactionCard } from "../../components/transactionRow";
import { BlockCard } from "../../components/block";

interface Props {
  data: Block[];
}

export function BlockchainView({ data }: Props) {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Blockchain View</h2>

      <div className="space-y-4 overflow-x-auto">
        {data.map((block) => (
          <div key={block.blockNumber} className="border rounded-lg shadow bg-white dark:bg-gray-800 p-4">
        
            <BlockCard data={block} />

        
            {block.transactions.length > 0 ? (
              <div className="mt-4 space-y-2">
                <h3 className="font-semibold">Transactions</h3>
                {block.transactions.map((tx) => (
                  <TransactionCard key={tx.txId} tx={tx} />
                ))}
              </div>
            ) : (
              <p className="mt-2 text-gray-500 dark:text-gray-400 text-sm">
                No transactions in this block.
              </p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}