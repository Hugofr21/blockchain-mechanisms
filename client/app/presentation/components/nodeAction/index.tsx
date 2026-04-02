import React, { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import type { NodeRow } from "../../../application/model/node";
import type { NodeAction } from "./types";
import { observabilityActions } from "../../../data/nodeActions";

import {
  Network,
  Database,
  Gavel,
  Skull,
  AlertTriangle,
  Bug,
  Power,
  ChevronRight,
} from "lucide-react";

interface Props {
  nodes: NodeRow[];
  onActionClick?: (node: NodeRow, action: NodeAction) => void;
}

function getActionIcon(actionId: string) {
  switch (actionId) {
    case "VIEW_ROUTING_TABLE":
      return <Network size={16} className="opacity-80" />;
    case "VIEW_BLOCKCHAIN":
      return <Database size={16} className="opacity-80" />;
    case "AUCTION_BID":
      return <Gavel size={16} className="opacity-80" />;
    case "CHAOS_SYBIL":
      return <Skull size={16} className="opacity-80" />;
    case "CHAOS_ECLIPSE":
      return <AlertTriangle size={16} className="opacity-80" />;
    case "CHAOS_POISONED_BLOCK":
      return <Bug size={16} className="opacity-80" />;
    case "SHUTDOWN_NODE":
      return <Power size={16} className="opacity-80" />;
    default:
      return <ChevronRight size={16} className="opacity-80" />;
  }
}

function isDangerAction(actionId: string) {
  return (
    actionId === "CHAOS_SYBIL" ||
    actionId === "CHAOS_ECLIPSE" ||
    actionId === "CHAOS_POISONED_BLOCK" ||
    actionId === "SHUTDOWN_NODE"
  );
}

export function NodeActionsDashboard({ nodes, onActionClick }: Props) {
  const [logs, setLogs] = useState<Record<string, string[]>>({});
  const [expandedNode, setExpandedNode] = useState<string | null>(null);

  const navigate = useNavigate();

  const safeActions = useMemo(
    () => observabilityActions.filter((a) => !isDangerAction(a.id)),
    []
  );

  const dangerActions = useMemo(
    () => observabilityActions.filter((a) => isDangerAction(a.id)),
    []
  );

  const handleClick = (node: NodeRow, action: NodeAction) => {
    setLogs((prev) => ({
      ...prev,
      [node.id]: [
        ...(prev[node.id] || []),
        `${action.label} @ ${new Date().toLocaleTimeString()}`,
      ],
    }));

    if (onActionClick) onActionClick(node, action);

    switch (action.id) {
      case "VIEW_BLOCKCHAIN":
        navigate(`/node/${node.httpPort}/blockchain`);
        break;
      case "AUCTION_BID":
        navigate(`/node/${node.httpPort}/auction`);
        break;
      case "VIEW_ROUTING_TABLE":
        navigate(`/node/${node.httpPort}/network`);
        break;
      default:
        break;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900 dark:text-white">
            Nodes Dashboard
          </h2>
          <p className="text-sm text-gray-600 dark:text-gray-400">
           Monitoring and operational actions per node.
          </p>
        </div>

        <div className="text-xs text-gray-500 dark:text-gray-400">
          Total nodes: <span className="font-semibold">{nodes.length}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5">
        {nodes.map((node) => {
          const isExpanded = expandedNode === node.id;

          return (
            <div
              key={node.id}
              className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm hover:shadow-md transition overflow-hidden"
            >
              {/* Header */}
              <div className="p-4 border-b border-gray-100 dark:border-gray-800">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
                      Node ID
                    </p>
                    <h3 className="text-sm font-semibold text-gray-900 dark:text-white truncate">
                      {node.id}
                    </h3>
                  </div>

                  <button
                    onClick={() =>
                      setExpandedNode(isExpanded ? null : node.id)
                    }
                    className="text-xs px-3 py-1 rounded-full border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 transition"
                  >
                    {isExpanded ? "Ocultar" : "Detalhes"}
                  </button>
                </div>

                <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-gray-600 dark:text-gray-400">
                  <div className="truncate">
                    <span className="font-medium">Host:</span>{" "}
                    <span className="font-mono">{node.host}</span>
                  </div>
                  <div className="truncate">
                    <span className="font-medium">HTTP:</span>{" "}
                    <span className="font-mono">{node.httpPort}</span>
                  </div>
                  <div className="truncate">
                    <span className="font-medium">Kademlia:</span>{" "}
                    <span className="font-mono">{node.port}</span>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="p-4 space-y-4">
                <div>
                  <p className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-2">
                    Observability
                  </p>

                  <div className="space-y-2">
                    {safeActions.map((action) => (
                      <button
                        key={action.id}
                        onClick={() => handleClick(node, action)}
                        className="w-full flex items-center justify-between gap-3 px-3 py-2 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 transition text-sm text-gray-900 dark:text-white"
                      >
                        <span className="flex items-center gap-2 text-left">
                          {getActionIcon(action.id)}
                          {action.label}
                        </span>

                        <ChevronRight size={16} className="opacity-60" />
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <p className="text-xs font-semibold text-red-500 uppercase tracking-wide mb-2">
                    Attack / Dangerous
                  </p>

                  <div className="space-y-2">
                    {dangerActions.map((action) => (
                      <button
                        key={action.id}
                        onClick={() => handleClick(node, action)}
                        className="w-full flex items-center justify-between gap-3 px-3 py-2 rounded-xl border border-red-200 dark:border-red-900 bg-red-50 dark:bg-red-950/30 hover:bg-red-100 dark:hover:bg-red-950/50 transition text-sm text-red-700 dark:text-red-300"
                      >
                        <span className="flex items-center gap-2 text-left">
                          {getActionIcon(action.id)}
                          {action.label}
                        </span>

                        <ChevronRight size={16} className="opacity-60" />
                      </button>
                    ))}
                  </div>
                </div>

                {/* Logs */}
                {logs[node.id]?.length > 0 && isExpanded && (
                  <div className="mt-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 p-3 text-xs">
                    <p className="font-semibold text-gray-700 dark:text-gray-300 mb-2">
                      Logs
                    </p>
                    <ul className="space-y-1 text-gray-600 dark:text-gray-400 max-h-40 overflow-y-auto">
                      {logs[node.id].slice().reverse().map((log, idx) => (
                        <li key={idx} className="font-mono">
                          {log}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}