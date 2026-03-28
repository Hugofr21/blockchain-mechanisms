import React from "react";
import type { Block } from "../../components/block/types";
import { BlockCard } from "../../components/block";
interface Props {
  data: Block[];
}

export function BlockchainView({ data }: Props) {
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Blockchain View</h2>

      <div className="space-y-4">
        {data.map((block) => (
          <BlockCard key={block.blockNumber} data={block} />
        ))}
      </div>
    </div>
  );
}