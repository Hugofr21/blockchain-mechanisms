import React, { useState, useEffect } from "react";
import { NodeActionsDashboard } from "../../components/nodeAction";
import type { NodeRow } from "../../../application/model/node";
import type { NodeAction } from "../../components/nodeAction/types";

interface Props {
  nodes: NodeRow[];
  onSimulateSybil: (nodeId: string) => Promise<any>;
  onSimulateEclipse: (nodeId: string) => Promise<any>;
  onSimulatePoison: (nodeId: string) => Promise<any>;
  onShutdownThisNode: (nodeId: string) => Promise<any>;
  onAddPeer: () => Promise<any>;
}

const LOG_STORAGE_KEY = "@dht-ledger/global-logs";
const GRAFANA_URL = "http://localhost:3000";

export function Dashboard({
  nodes,
  onSimulateSybil,
  onSimulateEclipse,
  onSimulatePoison,
  onShutdownThisNode,
  onAddPeer,
}: Props) {
  const [isDeploying, setIsDeploying] = useState<boolean>(false);
  const [globalLogs, setGlobalLogs] = useState<string[]>(() => {
    if (typeof window !== "undefined") {
      const savedLogs = localStorage.getItem(LOG_STORAGE_KEY);
      if (savedLogs) {
        try {
          return JSON.parse(savedLogs);
        } catch (e) {
          console.error("Falha ao desserializar logs gravados.");
        }
      }
    }
    return [];
  });

  useEffect(() => {
    localStorage.setItem(LOG_STORAGE_KEY, JSON.stringify(globalLogs));
  }, [globalLogs]);

  const handleClearLogs = () => {
    setGlobalLogs([]);
    localStorage.removeItem(LOG_STORAGE_KEY);
  };


  const handleAddPeer = async () => {
    const time = new Date().toLocaleTimeString();
    
    setGlobalLogs((prev) => [
      `[${time}] ORQUESTRADOR -> A instanciar nova réplica P2P na infraestrutura...`,
      ...prev,
    ]);

    setIsDeploying(true);

    try {
      const result = await onAddPeer();
      
      const successLog = `[${new Date().toLocaleTimeString()}] SUCESSO (ORQUESTRADOR):
          -> Estado: ${result.status}
          -> Mensagem: ${result.message}
          -> Contentor: ${result.metadata?.container}
          -> IP Atribuído: ${result.metadata?.ip}
          -> Portas: HTTP ${result.metadata?.httpPort} | RPC ${result.metadata?.rpcPort}`;
      
      setGlobalLogs((prev) => [successLog, ...prev]);
    } catch (err: any) {
      const errorData = err.response?.data;
      const errorMsg = errorData 
        ? `${errorData.message} | Detalhe: ${errorData.error}`
        : err.message || "Falha catastrófica de rede ou infraestrutura.";

      const failureLog = `[${new Date().toLocaleTimeString()}] FALHA CRÍTICA (ORQUESTRADOR):
        └── Diagnóstico: ${errorMsg}`
      
      setGlobalLogs((prev) => [failureLog, ...prev]);
       
    } finally {
      setIsDeploying(false);
    }
  };

  const handleOpenGrafana = () => {
    window.open(GRAFANA_URL, "_blank", "noopener,noreferrer");
  };

  const buildSimulationReport = (data: any): string => {
    if (!data) return "\n  ----> Nenhuma resposta recebida do contentor.";

    let report = "";

    if (data.attack) report += `\n   ----> Vetor de Ataque: ${data.attack}`;
    if (data.status) report += `\n   ---> Estado da Operação: ${data.status}`;
    if (data.acceptedNodes !== undefined)
      report += `\n  ----> Nós Infetados/Comprometidos: ${data.acceptedNodes}`;
    if (data.rejectedNodes !== undefined)
      report += `\n  ----> Nós Defendidos/Rejeitados: ${data.rejectedNodes}`;
    if (data.message) report += `\n  -> Mensagem do Servidor: ${data.message}`;

    return report !== "" ? report : `\n  -> Payload Bruto:\n${JSON.stringify(data, null, 4)}`;
  };

  const handleGlobalAction = async (node: NodeRow, action: NodeAction) => {
    const time = new Date().toLocaleTimeString();

    setGlobalLogs((prev) => [
      `[${time}] ${node.id} -> Iniciando propagação: ${action.label}...`,
      ...prev,
    ]);

    try {
      let result;

      switch (action.id) {
        case "CHAOS_SYBIL":
          result = await onSimulateSybil(node.httpPort);
          break;
        case "CHAOS_ECLIPSE":
          result = await onSimulateEclipse(node.httpPort);
          break;
        case "CHAOS_POISONED_BLOCK":
          result = await onSimulatePoison(node.httpPort);
          break;
        case "SHUTDOWN_NODE":
          result = await onShutdownThisNode(node.httpPort);
          break;
        default:
          return;
      }

      const formattedReport = buildSimulationReport(result);
      const successLog = `[${new Date().toLocaleTimeString()}] SUCESSO (${node.id}):${formattedReport}`;

      setGlobalLogs((prev) => [successLog, ...prev]);
    } catch (err: any) {
      const errorData = err.response?.data;
      const isTimeout = err.code === "ECONNABORTED" || err.message.includes("timeout");

      const errorMsg = isTimeout
        ? "O navegador abortou a conexão porque o servidor Java demorou demasiado tempo a processar a criptografia."
        : errorData?.error || err.message || "Falha catastrófica de infraestrutura.";

      const failureLog = `[${new Date().toLocaleTimeString()}] FALHA CRÍTICA (${node.id}):\n  └── Diagnóstico: ${errorMsg}`;

      setGlobalLogs((prev) => [failureLog, ...prev]);
    }
  };

  return (
    <main className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto p-6 space-y-10">
      <header className="flex flex-col gap-4">
          <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
            <div>
              <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">
                DHT Ledger Dashboard
              </h1>
              <p className="text-gray-600 dark:text-gray-400 text-sm max-w-2xl mt-1">
                Monitoring, infrastructure orchestration, and security simulation across replicated nodes.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-3">
              <button
                onClick={handleAddPeer}
                disabled={isDeploying}
                className={`px-4 py-2.5 rounded-xl font-semibold text-sm shadow transition flex items-center justify-center min-w-[160px]
                  ${
                    isDeploying
                      ? "bg-indigo-400 cursor-not-allowed text-white"
                      : "bg-indigo-600 hover:bg-indigo-700 text-white"
                  }`}
              >
                {isDeploying ? (
                  <span className="flex items-center gap-2">
                    <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    A instanciar...
                  </span>
                ) : (
                  "+ Adicionar Nó P2P"
                )}
              </button>

              <button
                onClick={handleOpenGrafana}
                className="px-4 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-white font-semibold text-sm shadow transition"
              >
                Abrir Grafana
              </button>
            </div>
          </div>
        </header>

        <section className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-6 space-y-6">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
              Node Actions
            </h2>
            <span className="text-xs px-3 py-1 rounded-full bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-200 w-fit">
              Simulation Mode
            </span>
          </div>

          <NodeActionsDashboard nodes={nodes} onActionClick={handleGlobalAction} />
        </section>

        <section className="rounded-2xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 shadow-sm p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              Global Logs
            </h2>
            <button
              onClick={handleClearLogs}
              className="text-xs px-3 py-1 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-200 transition"
            >
              Clear Logs
            </button>
          </div>

          {globalLogs.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No events recorded yet.
            </p>
          ) : (
            <div className="rounded-xl bg-gray-950 text-green-300 font-mono text-xs p-4 max-h-96 overflow-y-auto shadow-inner border border-gray-800">
              {globalLogs.map((log, idx) => (
                <div
                  key={idx}
                  className="whitespace-pre-wrap leading-relaxed pb-3 mb-3 border-b border-gray-800 last:border-0 last:mb-0 last:pb-0"
                >
                  {log}
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}