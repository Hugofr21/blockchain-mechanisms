import { apiClient } from "../lib/api";

export interface ChaosSimulationResponse {
    attack: string;
    acceptedNodes: number;
    rejectedNodes: number;
    status: string;
}

export interface ChaosTransactionResponse {
    status: string;
    message: string;
}

export interface ChaosTargetPayload {
    auctionId: string;
}

export const fetchSimulateSybilAttack = async (
    nodeId: string,
    signal?: AbortSignal
): Promise<ChaosSimulationResponse> => {
    const response = await apiClient.post<ChaosSimulationResponse>(
        "/api/chaos/sybil",
        {},
        { 
            headers: { 'X-Target-Node': nodeId },
            timeout: 60000,
            signal 
        }
    );
    return response.data;
};

export const fetchSimulateEclipseAttack = async (
    nodeId: string,
    signal?: AbortSignal
): Promise<ChaosSimulationResponse> => {
    const response = await apiClient.post<ChaosSimulationResponse>(
        "/api/chaos/eclipse",
        {},
        { 
            headers: { 'X-Target-Node': nodeId },
            timeout: 120000,
            signal 
        }
    );
    return response.data;
};

export const fetchSimulateDuplicateBids = async (
    nodeId: string,
    auctionId: string,
    signal?: AbortSignal
): Promise<ChaosTransactionResponse> => {
    const payload: ChaosTargetPayload = { auctionId };
    const response = await apiClient.post<ChaosTransactionResponse>(
        "/api/chaos/duplicate-bids",
        payload,
        { 
            headers: { 'X-Target-Node': nodeId },
            signal 
        }
    );
    return response.data;
};

export const fetchSimulateRollbackAttack = async (
    nodeId: string,
    auctionId: string,
    signal?: AbortSignal
): Promise<ChaosTransactionResponse> => {
    const payload: ChaosTargetPayload = { auctionId };
    const response = await apiClient.post<ChaosTransactionResponse>(
        "/api/chaos/rollback",
        payload,
        { 
            headers: { 'X-Target-Node': nodeId },
            timeout: 60000,
            signal 
        }
    );
    return response.data;
};

export const fetchSimulateStressBids = async (
    nodeId: string,
    auctionId: string,
    signal?: AbortSignal
): Promise<ChaosTransactionResponse> => {
    const payload: ChaosTargetPayload = { auctionId };
    const response = await apiClient.post<ChaosTransactionResponse>(
        "/api/chaos/stress-bids",
        payload,
        { 
            headers: { 'X-Target-Node': nodeId },
            signal 
        }
    );
    return response.data;
};

export const fetchSimulatePoisonedBlock = async (
    nodeId: string,
    signal?: AbortSignal
): Promise<ChaosTransactionResponse> => {
    const response = await apiClient.post<ChaosTransactionResponse>(
        "/api/chaos/poisoned-block",
        {},
        { 
            headers: { 'X-Target-Node': nodeId },
            signal 
        }
    );
    return response.data;
};