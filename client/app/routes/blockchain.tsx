import { useParams } from "react-router";
import { BlockchainView } from "~/presentation/pages/blockchainExplorer/blockchain";
import { useBlockDetails } from "~/infrastructure/hooks/blockHook";

export default function BlockchainRouter() {
  const { targetNodePort } = useParams<{ targetNodePort: string }>();
  const { blocks, isLoading, error } = useBlockDetails(targetNodePort || null);

  if (isLoading) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <p className="font-mono text-indigo-600 animate-pulse">
          A sincronizar livro-razão criptográfico com o contentor {targetNodePort}...
        </p>
      </main>
    );
  }

  if (error || !targetNodePort) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <div className="bg-red-100 text-red-700 p-4 rounded border border-red-400 font-mono">
          {error || "Anomalia de Roteamento: Nenhum nó detetado no endereço."}
        </div>
      </main>
    );
  }

  return (
    <main className="max-w-7xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-2">Blockchain Explorer</h1>
      <p className="text-sm text-gray-500 mb-6 font-mono">
        A interrogar a matriz de estado do contentor isolado: <span className="font-bold text-gray-800 dark:text-gray-200">{targetNodePort}</span>
      </p>
      
      <BlockchainView data={blocks} />
    </main>
  );
}