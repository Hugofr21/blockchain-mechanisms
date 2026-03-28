import React from "react";
import type { NodeRow } from "../../../application/model/node";

interface Props {
  data: NodeRow;
}

export const NodeCard: React.FC<Props> = ({ data }) => {
  return (
    <div className="node-card border rounded p-4 space-y-2">
      <h3 className="text-xl font-semibold">Node: {data.id}</h3>

      <p><strong>Host:</strong> {data.host}</p>
      <p><strong>Port:</strong> {data.port}</p>
      <p><strong>Nonce:</strong> {data.nonce}</p>
      <p><strong>Network Difficulty:</strong> {data.networkDifficulty}</p>

      <p>
        <strong>URL:</strong>{" "}
        <a
          className="text-blue-600 underline"
          href={`http://${data.host}:${data.port}`}
          target="_blank"
          rel="noreferrer"
        >
          http://{data.host}:{data.port}
        </a>
      </p>
    </div>
  );
};