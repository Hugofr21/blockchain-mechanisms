import React from "react";
import type { LogPayload } from "../../../application/model/log";
import { LogCard } from "./logCard";
import { FileText, ServerOff } from "lucide-react";

interface Props {
  logs: LogPayload[];
  title?: string;
}

export const LogsList: React.FC<Props> = ({ logs, title = "System Logs" }) => {
  if (!logs || logs.length === 0) {
    return (
      <div className="flex items-center gap-3 p-4 rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm">
        <ServerOff className="text-gray-400" size={18} />
        <p className="text-sm text-gray-600 dark:text-gray-400">
         No system records found.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <FileText size={18} className="text-indigo-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            {title}
          </h2>
        </div>
      </div>
      <div className="flex flex-col gap-5">
        {logs.map((logFile, index) => (
          <LogCard key={`${logFile.fileName}-${index}`} logFile={logFile} />
        ))}
      </div>
    </div>
  );
};