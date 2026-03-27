import React, { useState } from "react";
import { useNavigate } from "react-router";
import type { NodeRow } from "../../components/nodeSelector/types";
import type { NodeAction } from "./types";
import { testActions } from "../../data/nodeActions";

interface Props {
  nodes: NodeRow[];
  onActionClick?: (node: NodeRow, action: NodeAction) => void;
}

export function NodeActionsDashboard({ nodes, onActionClick }: Props) {
  const [logs, setLogs] = useState<Record<string, string[]>>({});
  const navigate = useNavigate();

  const handleClick = (node: NodeRow, action: NodeAction) => {
    setLogs((prev) => ({
      ...prev,
      [node.id]: [
        ...(prev[node.id] || []),
        `${action.label} executed at ${new Date().toLocaleTimeString()}`,
      ],
    }));

    if (onActionClick) {
      onActionClick(node, action);
    }

    switch(action.id){
        case "action-1":
          navigate("/blockchain");
          break;
        default:
          break;
  
    }
     
    
  };

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
        {nodes.map((node) => (
          <div
            key={node.id}
            className="border rounded-lg shadow p-4 bg-white dark:bg-gray-800"
          >
            <h3 className="font-semibold mb-2">{node.id}</h3>
            <p className="text-sm mb-4 text-gray-600 dark:text-gray-300">
              {node.host}:{node.port}
            </p>

            <ul className="space-y-2">
              {testActions.map((action) => (
                <li
                  key={action.id}
                  className="px-3 py-2 bg-indigo-100 dark:bg-indigo-700 rounded cursor-pointer hover:bg-indigo-200 dark:hover:bg-indigo-600 transition"
                  onClick={() => handleClick(node, action)}
                >
                  {action.label}
                </li>
              ))}
            </ul>

            {logs[node.id]?.length > 0 && (
              <div className="mt-4 p-2 bg-gray-50 dark:bg-gray-900 rounded max-h-40 overflow-y-auto text-xs">
                <strong>Action Logs:</strong>
                <ul className="list-disc list-inside">
                  {logs[node.id].map((log, idx) => (
                    <li key={idx}>{log}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}