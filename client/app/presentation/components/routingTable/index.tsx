import React from "react";
import type { NodeRow } from "../../../application/model/node";
import { NodeCard } from "../node";
import { Loader2, ServerOff, Network, ShieldCheck } from "lucide-react";
import { n } from "node_modules/react-router/dist/development/index-react-server-client-BcrVT7Dd.mjs";

interface Props {
  nodes: NodeRow[];
  loading?: boolean;
  title?: string;
}

export function NodeList({ nodes, loading = false, title = "Nodes" }: Props) {
  if (loading) {
    return (
      <div className="flex items-center gap-3 p-5 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm">
        <Loader2 className="animate-spin text-indigo-500" size={18} />
        <div>
          <p className="text-sm font-semibold text-gray-800 dark:text-gray-200">
            A sincronizar tabela de roteamento...
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400 font-mono">
            DHT discovery em execução
          </p>
        </div>
      </div>
    );
  }

  if (!nodes || nodes.length === 0) {
    return (
      <div className="p-6 rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm space-y-2">
        <div className="flex items-center gap-3">
          <ServerOff className="text-gray-400" size={20} />
          <p className="text-sm font-semibold text-gray-800 dark:text-gray-200">
            Nenhum node disponível
          </p>
        </div>

        <p className="text-xs text-gray-500 dark:text-gray-400">
          A rede não retornou peers ativos ou a tabela ainda não foi propagada.
        </p>

        <p className="text-xs font-mono text-gray-400 dark:text-gray-500">
          STATUS: ROUTING TABLE EMPTY
        </p>
      </div>
    );
  }
  return (
    <section className="space-y-5">
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Network size={18} className="text-indigo-500" />
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              {title}
            </h2>
          </div>

          <p className="text-sm text-gray-600 dark:text-gray-400">
            List of peers known by the distributed routing table.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-900">
            Nodes: <span className="font-semibold">{nodes.length}</span>
          </span>
        </div>
      </header>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
        {nodes.map((node) => {
          const endpoint = `http://${node.host}:${node.httpPort}`;

          return (
            <div key={node.id} className="space-y-2">
              <NodeCard data={node} />

              <div className="px-4 py-2 rounded-xl border border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-950">
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Endpoint HTTP
                </p>
                <p className="text-xs font-mono text-indigo-600 dark:text-indigo-400 break-all">
                  {endpoint}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}