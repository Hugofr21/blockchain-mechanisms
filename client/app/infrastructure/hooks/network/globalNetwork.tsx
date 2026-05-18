import { useState, useEffect, useCallback } from "react";
import type { NodeRow } from "../../../application/model/node";
import { fetchMyselfIdentity, fetchActivePeers } from "../../services/network";

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

  const [trigger, setTrigger] = useState<number>(0);

  const refetch = useCallback(() => {
    setTrigger((prev) => prev + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    async function executeTopologySweep() {
      try {
        const validTargets = await fetchActivePeers(controller.signal);

        if (!validTargets || validTargets.length === 0) {
          throw new Error(
            "A varredura topológica não detetou instâncias ativas na infraestrutura Docker.",
          );
        }

        const promises = validTargets.map((nodeId) =>
          fetchMyselfIdentity(nodeId, controller.signal),
        );

        const results = await Promise.allSettled(promises);
        const activeNodes: NodeRow[] = [];

        results.forEach((result, index) => {
          const targetPort = validTargets[index];

          if (result.status === "fulfilled" && result.value) {
            const rawData = result.value as any;

            activeNodes.push({
              id: rawData.peerId || `Peer-${index}`,
              host: rawData.host,
              port: rawData.port,
              difficulty: rawData.difficulty || 0,
              httpPort: targetPort,
            });
          } else {
            console.warn(
              `Node offline shutdown ${targetPort}: ${
                result.status === "rejected" ? result.reason : "Resposta inválida"
              }`,
            );
            activeNodes.push({
              id: `[OFFLINE] peer-${targetPort}`,
              host: "Desconhecido",
              port: "N/A",
              difficulty: 0,
              httpPort: targetPort,
            });
          }

        });

        setState({
          nodes: activeNodes,
          loading: false,
          error: null,
        });
      } catch (err: any) {
        if (err.name === "CanceledError" || err.name === "AbortError") return;

        setState({
          nodes: [],
          loading: false,
          error: err.message || "Falha ao mapear a topologia automatizada.",
        });
      }
    }

    executeTopologySweep();
    const intervalId = setInterval(() => {
      executeTopologySweep();
    }, 5000);
    return () => {
      clearInterval(intervalId);
      controller.abort();
    };
  }, [trigger]);

  return { ...state, refetch };
}
