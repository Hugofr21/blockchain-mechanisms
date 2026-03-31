import { useState, useEffect } from 'react';
import axios from 'axios';
import type { AuctionRow } from '../../application/model/auction';
import { 
    fetchGetAllAuctions, 
    fetchGetAuctionById, 
    fetchCreateAuction, 
    fetchPlaceBid,
    fetchTestAuctionThisNode
} from '../services/auctionService' 

export interface TransactionResponse {
    status: string;
    message: string;
    auctionId?: string;
}


export const useGetAllAuction = (nodeId: string | null) => {
    const [auctions, setAuctions] = useState<AuctionRow[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!nodeId) {
            setError("Identificador do nó alvo ausente. Impossível estabelecer túnel de comunicação.");
            setIsLoading(false);
            return;
        }

        const abortController = new AbortController();
        const id = nodeId;
        
        const loadAuctions = async () => {
            setIsLoading(true);
            setError(null);
            
            try {
                const data = await fetchGetAllAuctions(id, abortController.signal);
                setAuctions(data);
                setError(null);
            } catch (err: any) {
                if (axios.isCancel(err) || err.name === "AbortError" || err.name === "CanceledError") return; 
                setError(`Falha ao contactar o nó ${id}. O contentor pode estar corrompido ou offline.`);
            } finally {
                if (!abortController.signal.aborted) {
                    setIsLoading(false);
                }
            }
        };

        loadAuctions();

        return () => {
            abortController.abort();
        };
    }, [nodeId]); 

    return { auctions, isLoading, error };
};

export const useGetAuctionById = (nodeId: string | null, auctionId: string) => {
    const [auction, setAuction] = useState<AuctionRow | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!nodeId || !auctionId) {
            setError("Parâmetros de interrogação ausentes. Nó ou identificador do leilão não fornecidos.");
            setIsLoading(false);
            return;
        }

        const abortController = new AbortController();
        const id = nodeId;
        
        const loadAuction = async () => {
            setIsLoading(true);
            setError(null);
            
            try {
                const data = await fetchGetAuctionById(id, auctionId, abortController.signal);
                setAuction(data);
                setError(null);
            } catch (err: any) {
                if (axios.isCancel(err) || err.name === "AbortError" || err.name === "CanceledError") return; 
                setError(`Falha ao contactar o nó ${id} para extrair o leilão ${auctionId}.`);
            } finally {
                if (!abortController.signal.aborted) {
                    setIsLoading(false);
                }
            }
        };

        loadAuction();

        return () => {
            abortController.abort();
        };
    }, [nodeId, auctionId]); 

    return { auction, isLoading, error };
};

export const useCreateAuction = () => {
    const [response, setResponse] = useState<TransactionResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string, description: string, startingPrice: string) => {
        setIsLoading(true);
        setError(null);
        setResponse(null);

        try {
            const data = await fetchCreateAuction(nodeId, description, startingPrice);
            setResponse(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao criar leilão.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, response, isLoading, error };
};

export const usePlaceBid = () => {
    const [response, setResponse] = useState<TransactionResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string, auctionId: string, bidAmount: string) => {
        setIsLoading(true);
        setError(null);
        setResponse(null);

        try {
            const data = await fetchPlaceBid(nodeId, auctionId, bidAmount);
            setResponse(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao submeter licitação.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, response, isLoading, error };
};

export function useTestAuctionThisNode() {
    const [result, setResult] = useState<TransactionResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchTestAuctionThisNode(nodeId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao invocar o teste de carga de leilões no contentor.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}