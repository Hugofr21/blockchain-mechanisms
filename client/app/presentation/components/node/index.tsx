import React from "react";
import type { NodeRow } from "../../../application/model/node";
import { Server, Globe, Cpu, Hash, Radio } from "lucide-react";

interface Props {
  data: NodeRow;
}

export function NodeCard({ data }: Props) {
  const { id, host, port, difficulty, httpPort } = data;

  return (
    <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm hover:shadow-md transition overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Node
          </p>

          <h2 className="text-sm font-semibold text-gray-900 dark:text-white truncate">
            {id}
          </h2>
        </div>

        <div className="flex items-center gap-2 text-indigo-500">
          <Server size={18} />
        </div>
      </div>

      {/* Body */}
      <div className="p-4 space-y-3">
        <div className="grid grid-cols-2 gap-3 text-xs text-gray-600 dark:text-gray-400">
          <div className="flex items-center gap-2 min-w-0">
            <Globe size={14} className="opacity-70" />
            <span className="truncate">
              <span className="font-medium">Host:</span>{" "}
              <span className="font-mono">{host}</span>
            </span>
          </div>

          <div className="flex items-center gap-2">
            <Radio size={14} className="opacity-70" />
            <span>
              <span className="font-medium">HTTP:</span>{" "}
              <span className="font-mono">{httpPort}</span>
            </span>
          </div>

          <div className="flex items-center gap-2">
            <Cpu size={14} className="opacity-70" />
            <span>
              <span className="font-medium">Kademlia:</span>{" "}
              <span className="font-mono">{port}</span>
            </span>
          </div>

          <div className="flex items-center gap-2">
            <Hash size={14} className="opacity-70" />
            <span>
              <span className="font-medium">Difficulty:</span>{" "}
              <span className="font-mono">{difficulty}</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}