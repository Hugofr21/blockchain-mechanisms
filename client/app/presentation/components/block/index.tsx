import React from "react";
import { useNavigate, useParams } from "react-router";
import type { Block } from "../../../application/model/block";
import { Blocks, Hash, ArrowLeftRight, Clock, FileText, ChevronRight } from "lucide-react";

interface Props {
  data: Block;
}

export function BlockCard({ data }: Props) {
  const navigate = useNavigate();
  const { targetNodePort } = useParams<{ targetNodePort: string }>();

  const { numberBlock, currentBlockHash, header, transactions } = data;
  const formattedTimestamp = new Date(header.timestamp).toLocaleString();

  const handleClick = () => {
    if (!targetNodePort) return;
    navigate(`/node/${targetNodePort}/blockchain/${currentBlockHash}`);
  };

  return (
    <div
      onClick={handleClick}
      className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-sm hover:shadow-md transition cursor-pointer overflow-hidden"
    >
      {/* Header */}
      <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-gray-500 dark:text-gray-400">
            Block
          </p>

          <h2 className="text-sm font-semibold text-gray-900 dark:text-white">
            #{numberBlock}
          </h2>
        </div>

        <div className="flex items-center gap-2 text-indigo-500">
          <Blocks size={18} />
        </div>
      </div>

      {/* Body */}
      <div className="p-4 space-y-3">
        <div className="flex items-start gap-2">
          <Hash size={14} className="opacity-70 mt-0.5" />
          <p className="text-xs text-gray-600 dark:text-gray-400 break-all">
            <span className="font-medium">Hash:</span>{" "}
            <span className="font-mono">{currentBlockHash}</span>
          </p>
        </div>

        <div className="flex items-start gap-2">
          <ArrowLeftRight size={14} className="opacity-70 mt-0.5" />
          <p className="text-xs text-gray-600 dark:text-gray-400 break-all">
            <span className="font-medium">Prev:</span>{" "}
            <span className="font-mono">{header.previousBlockHash}</span>
          </p>
        </div>

        <div className="grid grid-cols-2 gap-3 text-xs text-gray-600 dark:text-gray-400">
          <div className="flex items-center gap-2">
            <Clock size={14} className="opacity-70" />
            <span className="truncate">{formattedTimestamp}</span>
          </div>

          <div className="flex items-center gap-2 justify-end">
            <FileText size={14} className="opacity-70" />
            <span>
              TX: <span className="font-semibold">{transactions.length}</span>
            </span>
          </div>
        </div>

        {/* Footer */}
        <div className="pt-2 flex items-center justify-between text-indigo-600 dark:text-indigo-300 text-xs font-semibold">
          <span>View details</span>
          <ChevronRight size={16} className="opacity-70" />
        </div>
      </div>
    </div>
  );
}