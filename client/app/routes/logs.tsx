import { useParams } from "react-router";
import { LogsPage } from "~/presentation/pages/logs";
import { useLogs } from "../infrastructure/hooks/network/eventNetwork";
import type { LogPayload } from "~/application/model/log";

export default function LogsRouter() {
  const { targetNodePort } = useParams<{ targetNodePort: string }>();
  
  const { logs, loading, error } = useLogs(targetNodePort || ""); 

  if (!targetNodePort) {
    return (
      <main className="max-w-7xl mx-auto p-6">
        <div className="bg-red-50 text-red-700 p-6 rounded-lg font-mono">
          Falha de Navegação: Identificador do contentor alvo ausente.
        </div>
      </main>
    );
  }

  if (error) {
    return <p className="p-6 font-mono text-red-600">Falha de I/O Remota: {error}</p>;
  }

  return (
    <>
      <LogsPage 
        targetNodePort={targetNodePort}
        dataLogs={logs ? [logs as LogPayload] : []}
        isSyncing={loading}
      />
    </>
  );
}