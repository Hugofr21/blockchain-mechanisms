import { useEffect, useState } from "react";
import type { NodeRow } from "../../../application/model/node";
import { fetchMyselfIdentity, fetchAllNeighbors, fetchListOfLogs } from "../../services/network";

type NetworkState = {
  myself: NodeRow | null;
  neighbors: NodeRow[];
  loading: boolean;
  error: string | null;
};

export function useNetworkData(nodeId: string | null) {
  const [state, setState] = useState<NetworkState>({
    myself: null,
    neighbors: [],
    loading: true,
    error: null,
  });

  useEffect(() => {
    if (!nodeId) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: "Identificador do nó ausente ou inválido."
      }));
      return;
    }

    const controller = new AbortController();

    async function load() {
      try {
        if (!nodeId) {
          setState((prev) => ({
            ...prev,
            loading: false,
            error: "Identificador do nó ausente ou inválido."
          }));
          return;
        }

        const [myself, neighbors] = await Promise.all([
          fetchMyselfIdentity(nodeId, controller.signal),
          fetchAllNeighbors(nodeId, controller.signal)
        ]);

        setState({
          myself,
          neighbors,
          loading: false,
          error: null,
        });
      } catch (err: any) {
        if (err.name === "CanceledError" || err.name === "AbortError") return;

        setState({
          myself: null,
          neighbors: [],
          loading: false,
          error: err.response?.data?.error || err.message || "Falha catastrófica ao extrair topologia do nó.",
        });
      }
    }

    load();

    return () => controller.abort();
  }, [nodeId]);

  return state;
}


export interface ResponseLogs {
  fileName: string;
  linesReturned: number;
  data: string[];
}


export const useLogs = (nodeId: string) => {
  const [logs, setLogs] = useState<ResponseLogs | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const abortController = new AbortController();

    const loadLogs = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchListOfLogs(nodeId, abortController.signal);
        setLogs(data);
      } catch (err: any) {
        if (err.name !== "AbortError") {
          setError(err.message || "Ocorreu uma falha sistémica durante a extração do fluxo de logs.");
        }
      } finally {
        setLoading(false);
      }
    };

    if (nodeId) {
      loadLogs();
    }

    return () => {
      abortController.abort();
    };
  }, [nodeId]);

  return { logs, loading, error };
};