import React from "react";
import type { Block } from "../../../application/model/block";
import { BlockCard } from "../../components/block";
import { Link } from "react-router";

interface Props {
  data: Block[];
}

export function BlockchainView({ data }: Props) {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Link
          to="/index"
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold rounded-lg 
                     border border-gray-200 bg-white text-gray-800 shadow-sm 
                     hover:bg-gray-100 transition
                     dark:bg-gray-900 dark:text-gray-100 dark:border-gray-700 dark:hover:bg-gray-800"
        >
          Back
        </Link>

        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          Blockchain View
        </h2>
      </div>

      <div className="space-y-4">
        {data.map((block) => (
          <BlockCard key={block.numberBlock} data={block} />
        ))}
      </div>
    </div>
  );
}