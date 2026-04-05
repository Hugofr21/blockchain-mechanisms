import { data } from "react-router";
import type { NodeRow } from "../../application/model/node";
import { apiClient } from "../lib/api"; 

export const fetchAllNeighbors = async (
    nodeId: string, 
    signal?: AbortSignal
): Promise<NodeRow[]> => {
    const response = await apiClient.get<any[]>("/api/network/neighbors", { 
        headers: { 'X-Target-Node': nodeId },
        signal 
    });
    
    return response.data.map((node) => ({
        id: node.peerId,
        host: node.host,
        port: node.port,
        difficulty: node.difficulty,
        httpPort: String(node.port) 
    }));
};

export const fetchMyselfIdentity = async (
    nodeId: string,
    signal?: AbortSignal
): Promise<NodeRow> => {
    const response = await apiClient.get<any>("/api/network/identity", { 
        headers: { 'X-Target-Node': nodeId },
        signal 
    });
  
    return {
        id: response.data.peerId,
        host: response.data.host,
        port: response.data.port,
        difficulty: response.data.difficulty,
        httpPort: String(response.data.port)
    };
};

interface ResponseShutdown{
    status: string,
    message: string
}

export const fetchShutdowThisNode = async(
    nodeId: string,
    signal?: AbortSignal
): Promise<ResponseShutdown> => { 
    const response = await apiClient.post<ResponseShutdown>( 
        "/api/network/shutdown", 
        {}, 
        { 
            headers: { 'X-Target-Node': nodeId },
            signal 
        }
    );

    return {
        status: response.data.status,
        message: response.data.message
    };
}


export interface ResponseLogs {
  fileName: string;
  linesReturned: number;
  nextOffset: number;
  data: string[];
}

export const fetchListOfLogs = async (
  nodeId: string,
  offset: number = 0,
  signal?: AbortSignal
): Promise<ResponseLogs> => {
  const response = await apiClient.get<ResponseLogs>("/api/network/logs", { 
    headers: { 'X-Target-Node': nodeId },
    params: { offset }, 
    signal 
  });
  
  return response.data;
};