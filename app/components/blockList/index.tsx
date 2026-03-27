import React from "react";
import type { Block } from "../block/types";
import "./style.css";

interface Props {
  blocks: Block[];
}

export function BlockListCard({ blocks }: Props) {
  return (
    <div className="block-list-container overflow-x-auto p-4">
      <div className="block-list flex space-x-4">
        {blocks.map((block) => {
          const { blockNumber, cachedHash, header } = block;
          const formattedTimestamp = new Date(header.timestamp).toLocaleString();

          return (
            <div
              key={blockNumber}
              className="block-card border rounded p-4 min-w-[250px] flex-shrink-0"
            >
              <header className="block-card-header mb-2">
                <h2 className="font-bold text-lg">Block #{blockNumber}</h2>
                <p className="text-sm break-all">Hash: {cachedHash}</p>
              </header>

              <div className="block-card-body text-sm">
                <h3 className="font-semibold mb-1">Header</h3>
                <p><strong>Previous Hash:</strong> {header.previousHash}</p>
                <p><strong>Merkle Root:</strong> {header.merkleRoot}</p>
                <p><strong>Timestamp:</strong> {formattedTimestamp}</p>
                <p><strong>Nonce:</strong> {header.nonce}</p>
                <p><strong>Difficulty:</strong> {header.difficulty}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}