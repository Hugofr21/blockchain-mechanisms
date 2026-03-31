import { apiClient } from '../lib/api';
import type { Block } from '../../application/model/block';

export const fetchBlockByHash = async (
    nodeId: string,
    blockHash: string,
    signal?: AbortSignal
): Promise<Block> => {
    const response = await apiClient.get<Block>(`/api/blocks/${blockHash}`, {
        headers: { 'X-Target-Node': nodeId },
        signal
    });
    return response.data;
};

export const fetchAllBlocks = async (
    nodeId: string,
    signal?: AbortSignal
): Promise<Block[]> => {
    const response = await apiClient.get<Block[]>('/api/blocks', {
        headers: { 'X-Target-Node': nodeId },
        signal
    });
    return response.data;
};