import React from "react";
import type { TransactionsRow } from "./types";

interface Props {
  tx: TransactionsRow;
}

export const TransactionCard: React.FC<Props> = ({ tx }) => {
  const formattedTime = new Date(tx.timestamp).toLocaleString();

  return (
    <div className="transaction-card">
      <h4>TxID: {tx.txId}</h4>
      <p><strong>Type:</strong> {tx.type}</p>
      <p><strong>Sender:</strong> {tx.sender}</p>
      <p><strong>Owner ID:</strong> {tx.ownerId}</p>
      <p><strong>Nonce:</strong> {tx.nonce}</p>
      <p><strong>Timestamp:</strong> {formattedTime}</p>
      <div>
        <strong>Data:</strong>
        <pre>{JSON.stringify(tx.data, null, 2)}</pre>
      </div>
      {tx.signature && <p><strong>Signature:</strong> {tx.signature}</p>}
    </div>
  );
};