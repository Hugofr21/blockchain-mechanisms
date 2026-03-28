import React, { useState } from "react";
import { NodeActionsDashboard } from "../../components/nodeAction";
import { nodes } from "../../../data/node";
import type { NodeRow } from "../../components/nodeSelector/types";
import type { NodeAction } from "../../components/nodeAction/types";

export function Dashboard() {
  const [globalLogs, setGlobalLogs] = useState<string[]>([]);

  const handleGlobalAction = (node: NodeRow, action: NodeAction) => {
    const logEntry = `[${new Date().toLocaleTimeString()}] ${node.id} -> ${action.label}`;
    setGlobalLogs((prev) => [logEntry, ...prev]);
  };

  return (
    <main className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto p-6 space-y-10">

        <header className="flex flex-col gap-2">
          <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">
            DHT Ledger Dashboard
          </h1>
          <p className="text-gray-600 dark:text-gray-400 text-sm max-w-2xl">
            Monitorização e simulação de eventos distribuídos em nós replicados.
          </p>
        </header>

        <section className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-6 space-y-6">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
              Node Actions
            </h2>
            <span className="text-xs px-3 py-1 rounded-full bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-200 w-fit">
              Simulation Mode
            </span>
          </div>

          <NodeActionsDashboard nodes={nodes} onActionClick={handleGlobalAction} />
        </section>

        <section className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              Global Logs
            </h2>

            <button
              onClick={() => setGlobalLogs([])}
              className="text-xs px-3 py-1 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-200 transition"
            >
              Clear Logs
            </button>
          </div>

          {globalLogs.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No events recorded yet.
            </p>
          ) : (
            <div className="rounded-xl bg-gray-950 text-green-300 font-mono text-xs p-4 max-h-64 overflow-y-auto shadow-inner border border-gray-800">
              {globalLogs.map((log, idx) => (
                <div key={idx} className="whitespace-pre-wrap leading-relaxed">
                  {log}
                </div>
              ))}
            </div>
          )}
        </section>

      </div>
    </main>
  );
}