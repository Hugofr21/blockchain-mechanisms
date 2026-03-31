import { useEffect, useState } from "react";
import type { NodeRow } from "../../../application/model/node";
import { fetchMyselfIdentity } from "../../services/network";

const INFRASTRUCTURE_TARGETS = [
  "bootstrap",
  "8001", "8002", "8003", "8004", "8005", 
  "8006", "8007", "8008", "8009", "8010"
];

type GlobalNetworkState = {
  nodes: NodeRow[];
  loading: boolean;
  error: string | null;
};

export function useGlobalNetworkData() {
  const [state, setState] = useState<GlobalNetworkState>({
    nodes: [],
    loading: true,
    error: null,
  });

  useEffect(() => {
    const controller = new AbortController();

    async function loadGlobalTopology() {
      try {
        setState((prev) => ({ ...prev, loading: true, error: null }));

        const promises = INFRASTRUCTURE_TARGETS.map(nodeId => 
          fetchMyselfIdentity(nodeId, controller.signal)
        );


        const results = await Promise.allSettled(promises);

        const activeNodes: NodeRow[] = [];
        
      results.forEach((result, index) => {
          if (result.status === "fulfilled" && result.value) {
            const rawData = result.value as any;
            
            activeNodes.push({
              id: rawData.peerId || `identidade-desconhecida-${index}`,
              host: rawData.host,
              port: rawData.port,
              difficulty: rawData.difficulty || 0, 
              httpPort: INFRASTRUCTURE_TARGETS[index], 
            });
          } else {
            console.warn(`[TELEMETRIA] Perda de sinal com o contentor ${INFRASTRUCTURE_TARGETS[index]}.`);
          }
        });

        if (activeNodes.length === 0) {
          throw new Error("Colapso total da infraestrutura. O API Gateway rejeitou todas as conexões ou a rede Docker está em baixo.");
        }

        setState({
          nodes: activeNodes,
          loading: false,
          error: null,
        });

      } catch (err: any) {
        if (err.name === "CanceledError") return;

        setState({
          nodes: [],
          loading: false,
          error: err.message || "Falha catastrófica ao estabelecer o varrimento global da rede.",
        });
      }
    }

    loadGlobalTopology();

    return () => controller.abort();
  }, []);

  return state;
}