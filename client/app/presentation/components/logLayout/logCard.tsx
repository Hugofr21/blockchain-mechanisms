import React, { useState, useEffect, useRef } from "react";
import type { LogPayload } from "../../../application/model/log";
import { FileText, ChevronDown, ChevronUp, Database, AlignLeft } from "lucide-react";

interface Props {
  logFile: LogPayload;
}

export const LogCard: React.FC<Props> = ({ logFile }) => {
  const [expanded, setExpanded] = useState(true); 
  const terminalRef = useRef<HTMLPreElement>(null);

  useEffect(() => {
    if (terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
    }
  }, [logFile.data]);

  return (
    <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex items-start justify-between gap-3 bg-gray-50 dark:bg-gray-800/50">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Node Log File
          </p>
          <h4 className="text-sm font-semibold text-gray-900 dark:text-white truncate">
            {logFile.fileName}
          </h4>
        </div>
        <div className="flex items-center gap-4 text-xs font-mono">
           <span className="flex items-center gap-1 text-gray-600 dark:text-gray-400">
              <AlignLeft size={14}/> Lines: <b className="text-indigo-600">{logFile.linesReturned}</b>
           </span>
           <span className="flex items-center gap-1 text-gray-600 dark:text-gray-400">
              <Database size={14}/> Offset: <b className="text-indigo-600">{logFile.nextOffset} B</b>
           </span>
        </div>
      </div>

      {/* Body */}
      <div className="p-4 space-y-3">
        <button
          onClick={() => setExpanded((prev) => !prev)}
          className="w-full flex items-center justify-between text-xs font-semibold text-indigo-600 dark:text-indigo-300 pt-2"
        >
          <span>{expanded ? "Ocultar Registos" : "Visualizar Registos"}</span>
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </button>

        {expanded && (
          <div className="space-y-3 pt-2">
            <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-900 p-3">
              <p className="text-xs uppercase tracking-wide text-gray-400 mb-2 flex justify-between">
                <span>Terminal Output</span>
                <span className="text-green-500 animate-pulse">Live</span>
              </p>
              <pre 
                ref={terminalRef}
                className="text-xs font-mono text-green-400 whitespace-pre-wrap break-all leading-relaxed max-h-96 overflow-y-auto"
              >
                {logFile.data.length > 0 
                  ? logFile.data.join("\n") 
                  : "A extrair matriz de dados..."}
              </pre>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};