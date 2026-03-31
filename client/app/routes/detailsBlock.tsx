import { useParams } from "react-router";
import BlockDetailsPage from "~/presentation/pages/blockchainExplorer/detailsBlock";
import { useBlockByHash } from "~/infrastructure/hooks/blockHook";

export default function BlockDetailsRouter() {
  const { targetNodePort, hash } = useParams<{ targetNodePort: string; hash: string }>();
  const { block, isLoading, error } = useBlockByHash(targetNodePort || null, hash || null);
  
  if (isLoading) {
    return (
      <main className="max-w-7xl mx-auto p-6">
         <p className="font-mono text-indigo-600 animate-pulse">A extrair bloco {hash} do contentor {targetNodePort}...</p>
      </main>
    );
  }

  if (error || !block) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <div className="bg-red-100 text-red-700 p-4 rounded font-mono">
           {error || "Bloco não encontrado."}
        </div>
      </main>
    );
  }

  return <BlockDetailsPage block={block} targetNodePort={targetNodePort!} />;
}