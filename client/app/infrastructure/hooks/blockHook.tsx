import { useState, useEffect } from 'react';
import axios from 'axios';
import type { Block } from '../../application/model/block';
import { fetchAllBlocks, fetchBlockByHash } from '../services/blockchainService';

export const useBlockDetails = (nodeId: string | null) => {
    const [blocks, setBlocks] = useState<Block[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!nodeId) {
            setError("Identificador do nó alvo ausente. Impossível estabelecer túnel de comunicação.");
            setIsLoading(false);
            return;
        }

        const abortController = new AbortController();
        
        const loadBlockchainData = async () => {
            setIsLoading(true);
            setError(null);
            
            try {
                const ledger = await fetchAllBlocks(nodeId, abortController.signal);
                setBlocks(ledger);
                setError(null);
            } catch (err: any) {
                if (axios.isCancel(err)) {
                    return; 
                }
                setError(`Falha ao contactar o nó ${nodeId}. O contentor pode estar corrompido ou offline.`);
            } finally {
                if (!abortController.signal.aborted) {
                    setIsLoading(false);
                }
            }
        };

        loadBlockchainData();

        return () => {
            abortController.abort();
        };
    }, [nodeId]); 

    return { blocks, isLoading, error };
};



export const useBlockByHash = (nodeId: string | null, blockHash: string | null) => {
    const [block, setBlock] = useState<Block>();
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!nodeId || !blockHash) {
            setError("Parâmetros de interrogação ausentes. Nó ou Hash não fornecidos.");
            setIsLoading(false);
            return;
        }

        const abortController = new AbortController();
        
        const loadBlockData = async () => {
            setIsLoading(true);
            setError(null);
            
            try {
               
                const fetchedBlock = await fetchBlockByHash(nodeId, blockHash, abortController.signal);
                setBlock(fetchedBlock);
                setError(null);
            } catch (err: any) {
                if (axios.isCancel(err)) {
                    return; 
                }
                
                setError(`Falha ao contactar o nó ${nodeId}. Bloco não encontrado ou contentor offline.`);
            } finally {
                if (!abortController.signal.aborted) {
                    setIsLoading(false);
                }
            }
        };

        loadBlockData();

        return () => {
            abortController.abort();
        };
    }, [nodeId, blockHash]); 

    return { block, isLoading, error };
};