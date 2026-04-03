import React from "react";
import { Link } from "react-router"; 
import { BlockDetailsView } from "~/presentation/components/block/viewDetails/blockDetailsView";
import type { Block } from "../../../application/model/block";

interface Props {
  block: Block;
  targetNodePort: string;
}


export default function BlockDetailsPage({ block, targetNodePort }: Props) {
  return (
    <main className="max-w-7xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <Link
          to={`/node/${targetNodePort}/blockchain`}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold rounded-lg 
                     border border-gray-200 bg-white text-gray-800 shadow-sm 
                     hover:bg-gray-100 transition
                     dark:bg-gray-900 dark:text-gray-100 dark:border-gray-700 dark:hover:bg-gray-800"
        >
          Back
          <span className="font-mono text-indigo-600 dark:text-indigo-400">
            [{targetNodePort}]
          </span>
        </Link>
      </div>

      <BlockDetailsView block={block} />
    </main>
  );
}