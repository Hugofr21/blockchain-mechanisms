import React, { useState } from "react";
import type { TransactionsRow } from "../../../application/model/transaction";
import {
  FileText,
  Hash,
  User,
  Shield,
  Clock,
  ChevronDown,
  ChevronUp,
  KeyRound,
  Database,
} from "lucide-react";

interface Props {
  tx: TransactionsRow;
}

export const TransactionCard: React.FC<Props> = ({ tx }) => {
  const formattedTime = new Date(tx.timestamp).toLocaleString();
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Transaction
          </p>

          <h4 className="text-sm font-semibold text-gray-900 dark:text-white truncate">
            {tx.txId}
          </h4>
        </div>

        <div className="text-indigo-500">
          <FileText size={18} />
        </div>
      </div>

      {/* Body */}
      <div className="p-4 space-y-3">
        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <Hash size={14} className="opacity-70" />
          <span className="break-all">
            <span className="font-medium">TxID:</span>{" "}
            <span className="font-mono">{tx.txId}</span>
          </span>
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <Database size={14} className="opacity-70" />
          <span>
            <span className="font-medium">Type:</span>{" "}
            <span className="font-mono font-semibold text-indigo-600 dark:text-indigo-300">
              {tx.type}
            </span>
          </span>
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <User size={14} className="opacity-70" />
          <span className="break-all">
            <span className="font-medium">Sender:</span>{" "}
            <span className="font-mono">{tx.senderId}</span>
          </span>
        </div>

        <div className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
          <Shield size={14} className="opacity-70" />
          <span className="break-all">
            <span className="font-medium">Data Sign:</span>{" "}
            <span className="font-mono">{tx.dataSign}</span>
          </span>
        </div>

        <div className="grid grid-cols-2 gap-3 text-xs text-gray-600 dark:text-gray-400 pt-1">
          <div className="flex items-center gap-2">
            <KeyRound size={14} className="opacity-70" />
            <span>
              Nonce: <span className="font-mono font-semibold">{tx.nonce}</span>
            </span>
          </div>

          <div className="flex items-center gap-2 justify-end">
            <Clock size={14} className="opacity-70" />
            <span className="truncate">{formattedTime}</span>
          </div>
        </div>

        {/* Expand Data */}
        <button
          onClick={() => setExpanded((prev) => !prev)}
          className="w-full flex items-center justify-between text-xs font-semibold text-indigo-600 dark:text-indigo-300 pt-2"
        >
          <span>{expanded ? "Hide details" : "View details"}</span>
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </button>

        {expanded && (
          <div className="space-y-3 pt-2">
            <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-950 p-3">
              <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-2">
                Data Payload
              </p>

              <pre className="text-xs font-mono text-gray-700 dark:text-gray-300 whitespace-pre-wrap break-all">
                {JSON.stringify(tx.data, null, 2)}
              </pre>
            </div>

            {tx.signatureBase64 && (
              <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-950 p-3">
                <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400 mb-2">
                  Signature
                </p>

                <p className="text-xs font-mono text-gray-700 dark:text-gray-300 break-all">
                  {tx.signatureBase64}
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};