import { useState } from "react";
import { 
    fetchSimulateSybilAttack, 
    fetchSimulateEclipseAttack,
    fetchSimulateDuplicateBids,
    fetchSimulateRollbackAttack,
    fetchSimulateStressBids,
    fetchSimulatePoisonedBlock,
} from "../../services/enviroment";

import { fetchShutdowThisNode } from "~/infrastructure/services/network";

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

export function useSimulateSybilAttack() {
    const [result, setResult] = useState<ChaosSimulationResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchSimulateSybilAttack(nodeId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao executar simulação de ataque Sybil.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}

export function useSimulateEclipseAttack() {
    const [result, setResult] = useState<ChaosSimulationResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchSimulateEclipseAttack(nodeId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao executar simulação de ataque Eclipse.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}

export function useSimulateDuplicateBids() {
    const [result, setResult] = useState<ChaosTransactionResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string, auctionId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchSimulateDuplicateBids(nodeId, auctionId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao injetar licitações duplicadas.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}

export function useSimulateRollbackAttack() {
    const [result, setResult] = useState<ChaosTransactionResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string, auctionId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchSimulateRollbackAttack(nodeId, auctionId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao simular reversão da cadeia de blocos.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}

export function useSimulateStressBids() {
    const [result, setResult] = useState<ChaosTransactionResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string, auctionId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchSimulateStressBids(nodeId, auctionId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao submeter teste de carga de licitações.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}

export function useSimulatePoisonedBlock() {
    const [result, setResult] = useState<ChaosTransactionResponse | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchSimulatePoisonedBlock(nodeId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao propagar bloco corrompido na rede.";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}



interface ResponseShutdown{
    status: string,
    message: string
}

export function useSimulateShutDownThisNode() {
    const [result, setResult] = useState<ResponseShutdown | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const execute = async (nodeId: string) => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await fetchShutdowThisNode(nodeId);
            setResult(data);
            return data;
        } catch (err: any) {
            const errorMessage = err.response?.data?.error || err.message || "Falha catastrófica ao executar shutdown";
            setError(errorMessage);
            throw err;
        } finally {
            setIsLoading(false);
        }
    };

    return { execute, result, isLoading, error };
}