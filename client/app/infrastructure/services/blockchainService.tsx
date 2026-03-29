import { apiClient } from '../lib/api';
import type { Block } from '../../application/model/block';

export const fetchBlockByHash = async (blockHash: string, signal?: AbortSignal): Promise<Block> => {
    const response = await apiClient.get<Block>(`/blockchain/blocks/${blockHash}`, { signal });
    return response.data;
};

export const fetchBlockByNumber = async (blockNumber: number, signal?: AbortSignal): Promise<Block> => {
    const response = await apiClient.get<Block>(`/blockchain/height/${blockNumber}`, { signal });
    return response.data;
};

export const fetchAllBlocks = async (signal?: AbortSignal): Promise<Block[]> => {
    const response = await apiClient.get<Block[]>('/blockchain', { signal });
    return response.data;
};