import React from "react";
import type { NodeRow } from "../../../application/model/node";
import { NodeCard } from "../node";
import { Loader2, ServerOff, Network } from "lucide-react";

interface Props {
  nodes: NodeRow[];
  loading?: boolean;
  title?: string;
}

export function NodeList({ nodes, loading = false, title = "Nodes" }: Props) {
  if (loading) {
    return (
      <div className="flex items-center gap-3 p-4 rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm">
        <Loader2 className="animate-spin text-indigo-500" size={18} />
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Carregando nodes...
        </p>
      </div>
    );
  }

  if (!nodes || nodes.length === 0) {
    return (
      <div className="flex items-center gap-3 p-4 rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm">
        <ServerOff className="text-gray-400" size={18} />
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Nenhum node encontrado.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Network size={18} className="text-indigo-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            {title}
          </h2>
        </div>

        <span className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 bg-white dark:bg-gray-900">
          Total: <span className="font-semibold">{nodes.length}</span>
        </span>
      </div>

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
        {nodes.map((node) => (
          <NodeCard key={node.id} data={node} />
        ))}
      </div>
    </div>
  );
}