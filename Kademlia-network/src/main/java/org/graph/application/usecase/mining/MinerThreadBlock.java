package org.graph.application.usecase.mining;


import org.graph.domain.valueobject.utils.HashUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public record MinerThreadBlock(int threadId, long startNonce, long nonceRange, String data, int difficulty,
                               AtomicBoolean found) implements Callable<MiningResultBlock> {
    public MinerThreadBlock {
        if (difficulty < 1) {
            throw new IllegalArgumentException("difficulty must be >= 1 (hex digits).");
        }
    }

    @Override
    public MiningResultBlock call() {
        String target = new String(new char[difficulty]).replace('\0', '0');
        long endNonce = startNonce + nonceRange;
        long attempts = 0;

        for (long nonce = startNonce; nonce < endNonce && !found.get(); nonce++) {
            if (attempts % 10000 == 0 && found.get()) return null;

            attempts++;
            String hash = HashUtils.calculateSha256(data + nonce);

            if (hash != null && hash.substring(0, difficulty).equals(target)) {
                found.set(true);
                return new MiningResultBlock(nonce, hash, threadId, attempts);
            }
        }
        return null;
    }
}