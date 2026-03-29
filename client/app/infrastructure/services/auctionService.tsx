import { apiClient } from '../lib/api';
import type { AuctionRow } from '../../application/model/auction';

export const fetchGetByAll = async (signal?: AbortSignal): Promise<AuctionRow> => {
    const response = await apiClient.get<AuctionRow>(`/auctions`, { signal });
    return response.data;
};