import React from "react";
import { useParams, Link } from "react-router";
import { blocks } from "~/data/block";
import { BlockDetailsView } from "~/components/block/viewDetails/blockDetailsView";

export default function BlockDetailsPage() {
  const { height } = useParams<{ height: string }>();
  const blockNumber = Number(height);
  const block = blocks.find((b) => b.blockNumber === blockNumber);

  if (!block) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <h1 className="text-2xl font-bold">Block not found</h1>
        <Link to="/blockchain" className="text-indigo-600 hover:underline">
          Back to Blockchain
        </Link>
      </main>
    );
  }

  return (
    <main className="max-w-7xl mx-auto p-6">
      <Link to="/blockchain" className="text-sm text-indigo-600 hover:underline">
        ← Back
      </Link>
      <BlockDetailsView block={block} />
    </main>
  );
}