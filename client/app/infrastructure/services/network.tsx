import { apiClient } from "../lib/api";
import type { NodeRow } from "../../application/model/node";


export const fetchGetAllNodes = async (
  signal?: AbortSignal
): Promise<NodeRow[]> => {
  const response = await apiClient.get<NodeRow[]>("/nodes", { signal });
  return response.data;
};


export const fetchGetNodeById = async (
  nodeId: string,
  signal?: AbortSignal
): Promise<NodeRow> => {
  const response = await apiClient.get<NodeRow>(`/nodes/${nodeId}`, { signal });
  return response.data;
};

export const fetchGetMyself = async (
  signal?: AbortSignal
): Promise<NodeRow> => {
  const response = await apiClient.get<NodeRow>("/nodes/me", { signal });
  return response.data;
};