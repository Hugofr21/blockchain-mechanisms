import { BlockchainView } from "~/pages/blockchainExplorer/blockchain";
import { blocks } from "~/data/block";

export default function BlockchainPage() {
  return (
    <main className="max-w-7xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Blockchain Explorer</h1>
      <BlockchainView data={blocks} />
    </main>
  );
}