package org.graph.infrastructure.p2p;

import org.graph.domain.application.pow.MinerThread;
import org.graph.domain.application.pow.MiningResult;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinerOrchestrator {

    // Configuração de dificuldade (em produção, isso viria de um arquivo de config ou da rede)
    private static final int DIFFICULTY = 2;

    public static MiningResult executeMining(PublicKey publicKey) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        AtomicBoolean found = new AtomicBoolean(false);
        List<MinerThread> tasks = new ArrayList<>();

        // Espaço de busca: Long.MAX_VALUE distribuído entre as threads
        long rangePerThread = Long.MAX_VALUE / cores;

        System.out.println("[INFO] Starting Mining process with difficulty " + DIFFICULTY + " on " + cores + " cores.");

        for (int i = 0; i < cores; i++) {
            long startNonce = i * rangePerThread;
            tasks.add(new MinerThread(i, startNonce, rangePerThread, publicKey, DIFFICULTY, found));
        }

        try {
            // invokeAny bloqueia até que UMA das tarefas retorne com sucesso (sem lançar exceção).
            // Automaticamente cancela as outras tarefas pendentes quando a primeira termina.
            MiningResult result = executor.invokeAny(tasks);
            return result;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to generate valid NodeId via PoW", e);
        } finally {
            // Garante que o pool de threads seja destruído para não impedir o encerramento da JVM
            executor.shutdownNow();
        }
    }

    public static int getDifficulty() {
        return DIFFICULTY;
    }
}