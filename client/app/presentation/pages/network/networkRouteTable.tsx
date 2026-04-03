import React from "react";
import type { NodeRow } from "../../../application/model/node";
import { NodeList } from "../../components/routingTable/index";
import { Link } from "react-router";

interface Props {
  nodes: NodeRow[];
  loading?: boolean;
  myself?: NodeRow | null;
}

export function NetworkPage({ nodes, loading = false, myself }: Props) {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Link
          to="/index"
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold rounded-lg border border-gray-200 
          bg-white text-gray-800 shadow-sm hover:bg-gray-100 transition 
          dark:bg-gray-900 dark:text-gray-100 dark:border-gray-700 dark:hover:bg-gray-800"
        >
          Back
        </Link>

        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          Routing Table
        </h2>
      </div>

      {myself && (
        <div className="p-4 border rounded-xl bg-white dark:bg-gray-900 dark:border-gray-700">
          <p className="text-sm text-gray-600 dark:text-gray-400">
            <strong>My Identity:</strong> {myself.id} ({myself.host}:{myself.port})
          </p>
        </div>
      )}

      <NodeList nodes={nodes} loading={loading} />
    </div>
  );
}