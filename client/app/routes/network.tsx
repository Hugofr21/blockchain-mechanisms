import { useParams } from "react-router";
import { NetworkPage } from "~/presentation/pages/network/networkRouteTable";
import { useNetworkData } from "~/infrastructure/hooks/network/eventNetwork";

export default function NetworkRouter() {
  const { targetNodePort } = useParams<{ targetNodePort: string }>();
  const { neighbors, myself, loading, error } = useNetworkData(targetNodePort || null);

  if (loading) {
    return (
      <main className="flex items-center justify-center min-h-[50vh] bg-transparent">
        <p className="text-indigo-600 animate-pulse font-mono tracking-wide">
          A extrair topologia Kademlia (KBuckets) do contentor {targetNodePort}...
        </p>
      </main>
    );
  }

  if (error || !targetNodePort) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-6 rounded-lg border border-red-300 shadow-sm font-mono">
          <h2 className="text-lg font-bold mb-2">Falha de Observabilidade Topológica</h2>
          <p>{error || "Anomalia de Roteamento: Nenhum contentor alvo detetado no endereço."}</p>
        </div>
      </main>
    );
  }

  return <NetworkPage nodes={neighbors} myself={myself} loading={loading} />;
}