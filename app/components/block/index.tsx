import React from "react";
import type { Block } from "./types.tsx";
import "./systle.css"; 

interface Props {
  data: Block;
}

export function BlockCard({ data }: Props) {
  const { blockNumber, cachedHash, header } = data;

  const formattedTimestamp = new Date(header.timestamp).toLocaleString();

  return (
    <div className="block-card">
      <header className="block-card-header">
        <h2>Block #{blockNumber}</h2>
        <p>Hash: {cachedHash}</p>
      </header>

      <div className="block-card-body">
        <h3>Header</h3>
        <p><strong>Previous Hash:</strong> {header.previousHash}</p>
        <p><strong>Merkle Root:</strong> {header.merkleRoot}</p>
        <p><strong>Timestamp:</strong> {formattedTimestamp}</p>
        <p><strong>Nonce:</strong> {header.nonce}</p>
        <p><strong>Difficulty:</strong> {header.difficulty}</p>
      </div>
    </div>
  );
}