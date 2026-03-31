import React from "react";
import type { NodeRow } from "../../../application/model/node";
import { NodeList } from "../../components/routingTable/index";

interface Props {
  nodes: NodeRow[];
  loading?: boolean;
  myself?: NodeRow | null;
}

export function NetworkPage({ nodes, loading = false, myself }: Props) {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Routing Table</h2>
      {myself && (
        <div className="p-4 border rounded-xl bg-white dark:bg-gray-900">
          <p className="text-sm text-gray-600 dark:text-gray-400">
            <strong>My Identity:</strong> {myself.id} ({myself.host}:{myself.port})
          </p>
        </div>
      )}
      <NodeList nodes={nodes} loading={loading} />
    </div>
  );
}