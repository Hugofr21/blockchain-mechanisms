package org.graph.infrastructure.network;

import org.graph.application.usecase.mining.MinerThread;
import org.graph.application.usecase.mining.MiningResult;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.graph.adapter.utils.Constants.NETWORK_DIFFICULTY;

public class MinerOrchestrator {

    public static MiningResult executeMining(PublicKey publicKey) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        AtomicBoolean found = new AtomicBoolean(false);
        List<MinerThread> tasks = new ArrayList<>();

        long rangePerThread = Long.MAX_VALUE / cores;

        System.out.println("[INFO] Starting Mining process with difficulty " + NETWORK_DIFFICULTY + " on " + cores + " cores.");

        for (int i = 0; i < cores; i++) {
            long startNonce = i * rangePerThread;
            tasks.add(new MinerThread(i, startNonce, rangePerThread, publicKey.getEncoded(), NETWORK_DIFFICULTY, found));
        }

        try {
            return executor.invokeAny(tasks);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to generate valid NodeId via PoW", e);
        } finally {
            executor.shutdownNow();
        }
    }
}