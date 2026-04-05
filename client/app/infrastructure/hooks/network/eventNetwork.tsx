import { useEffect, useState, useRef } from "react";
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

export const useLogs = (nodeId: string, pollIntervalMs: number = 3000) => {
  const [logData, setLogData] = useState<ResponseLogs | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);


  const currentOffset = useRef<number>(0);

  useEffect(() => {
    if (!nodeId) return;

    const abortController = new AbortController();

    const pollLogs = async () => {
      try {
        const data = await fetchListOfLogs(
          nodeId, 
          currentOffset.current, 
          abortController.signal
        );

        if (data.linesReturned > 0) {
          setLogData((prev) => {
        
            if (!prev) return data;
            
       
            return {
              ...data,
              data: [...prev.data, ...data.data],
              linesReturned: prev.linesReturned + data.linesReturned
            };
          });
          
      
          currentOffset.current = data.nextOffset;
        }
        
        setError(null);
      } catch (err: any) {
        if (err.name !== "AbortError") {
          setError(err.message || "Falha na sincronização contínua do fluxo de logs.");
        }
      } finally {
        setLoading(false);
      }
    };


    pollLogs();

  
    const intervalId = setInterval(pollLogs, pollIntervalMs);

    return () => {
      clearInterval(intervalId);
      abortController.abort();
    };
  }, [nodeId, pollIntervalMs]);

  return { logs: logData, loading, error };
};