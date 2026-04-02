import React from "react";
import { Link } from "react-router"; // Removido useParams daqui, já está no Router
import { BlockDetailsView } from "~/presentation/components/block/viewDetails/blockDetailsView";
import type { Block } from "../../../application/model/block";

interface Props {
  block: Block;
  targetNodePort: string;
}

export default function BlockDetailsPage({ block, targetNodePort }: Props) {
  return (
    <main className="max-w-7xl mx-auto p-6">
  
      <Link to={`/node/${targetNodePort}/blockchain`} className="text-sm text-indigo-600 hover:underline mb-4 inline-block font-mono">
         Back {targetNodePort}
      </Link>
      <BlockDetailsView block={block} />
    </main>
  );
}