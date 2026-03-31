import { apiClient } from '../lib/api';
import type { AuctionRow } from '../../application/model/auction';

export interface TransactionResponse {
    status: string;
    message: string;
    auctionId?: string;
}

export const fetchGetAllAuctions = async (
    nodeId: string,
    signal?: AbortSignal
): Promise<AuctionRow[]> => {
    const response = await apiClient.get<AuctionRow[]>('/api/auctions', {
        headers: { 'X-Target-Node': nodeId },
        signal
    });
    return response.data;
};

export const fetchGetAuctionById = async (
    nodeId: string,
    auctionId: string, 
    signal?: AbortSignal
): Promise<AuctionRow> => {
    const response = await apiClient.get<AuctionRow>(`/api/auctions/${auctionId}`, {
        headers: { 'X-Target-Node': nodeId },
        signal
    });
    return response.data;
};

export const fetchCreateAuction = async (
    nodeId: string,
    description: string, 
    startingPrice: string, 
    signal?: AbortSignal
): Promise<TransactionResponse> => {
    const payload = { description, startingPrice };
    const response = await apiClient.post<TransactionResponse>('/api/auctions', payload, {
        headers: { 'X-Target-Node': nodeId },
        signal
    });
    return response.data;
};

export const fetchPlaceBid = async (
    nodeId: string,
    auctionId: string, 
    bidAmount: string, 
    signal?: AbortSignal
): Promise<TransactionResponse> => {
    const payload = { bidAmount };
    const response = await apiClient.post<TransactionResponse>(`/api/auctions/${auctionId}/bids`, payload, {
        headers: { 'X-Target-Node': nodeId },
        signal
    });
    return response.data;
};

export const fetchTestAuctionThisNode = async (
    nodeId: string,
    signal?: AbortSignal
): Promise<TransactionResponse> => {
    const response = await apiClient.post<TransactionResponse>(
        `/api/auctions/test`, 
        null, 
        {
            headers: { 'X-Target-Node': nodeId },
            signal
        }
    );
    return response.data;
};