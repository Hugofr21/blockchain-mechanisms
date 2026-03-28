// components/block/BlockCard.tsx
import React from "react";
import { useNavigate } from "react-router";
import type { Block } from "./types";

interface Props {
  data: Block;
}

export function BlockCard({ data }: Props) {
  const navigate = useNavigate(); 

  const { blockNumber, cachedHash, header, transactions } = data;
  const formattedTimestamp = new Date(header.timestamp).toLocaleString();

  const handleClick = () => {
    navigate(`/blockchain/${blockNumber}`);
  };

  return (
    <div
      onClick={handleClick}
      className="border rounded-xl shadow-sm p-4 bg-white dark:bg-gray-900 cursor-pointer hover:shadow-md hover:border-indigo-400 transition space-y-2"
    >
      <h2 className="text-xl font-bold text-gray-900 dark:text-white">
        Block #{blockNumber}
      </h2>
      <p className="text-xs text-gray-600 dark:text-gray-400 break-all">
        Hash: <span className="font-mono">{cachedHash}</span>
      </p>
      <p className="text-xs text-gray-600 dark:text-gray-400 break-all">
        Prev: <span className="font-mono">{header.previousHash}</span>
      </p>
      <p className="text-sm text-gray-700 dark:text-gray-300">
        <strong>Timestamp:</strong> {formattedTimestamp}
      </p>
      <p className="text-sm text-gray-700 dark:text-gray-300">
        <strong>Tx Count:</strong> {transactions.length}
      </p>
      <p className="text-xs text-indigo-600 dark:text-indigo-300 font-semibold pt-1">
        Click to view details +
      </p>
    </div>
  );
}