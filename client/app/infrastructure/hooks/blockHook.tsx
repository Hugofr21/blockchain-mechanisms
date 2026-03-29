import { useState, useEffect } from 'react';
import axios from 'axios';
import type { Block } from '../../application/model/block';
import { fetchAllBlocks } from '../services/blockchainService';

export const useBlockDetails = () => {
    const [blocks, setBlocks] = useState<Block[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {

        const abortController = new AbortController();
        
        const loadBlockchainData = async () => {
            setIsLoading(true);
            setError(null);
            
            try {
                const ledger = await fetchAllBlocks(abortController.signal);
                setBlocks(ledger);
                setError(null);
            } catch (err: any) {
                if (axios.isCancel(err)) {
                    return; 
                }
                setError("Failed to locate the block on the selected node. The replica may be out of sync or the block was dropped in a fork.");
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
    }, []);

    return { blocks, isLoading, error };
};