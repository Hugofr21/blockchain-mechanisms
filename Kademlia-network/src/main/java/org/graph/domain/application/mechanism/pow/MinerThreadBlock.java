package org.graph.domain.application.mechanism.pow;


import org.graph.domain.utils.HashUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public record MinerThreadBlock(int threadId, int startNonce, int nonceRange, String data, int difficulty,
                               AtomicBoolean found) implements Callable<MiningResultBlock> {
    public MinerThreadBlock {

        if (difficulty < 1) {
            throw new IllegalArgumentException("difficulty must be >= 1 (hex digits).");
        }

    }

    @Override
    public MiningResultBlock call() {
        String target = new String(new char[difficulty]).replace('\0', '0');
        int endNonce = startNonce + nonceRange;
        long attempts = 0;

        for (int nonce = startNonce; nonce < endNonce && !found.get(); nonce++) {
            attempts++;
            String hash = HashUtils.calculateSha256(data + nonce);

            if (hash != null && hash.substring(0, difficulty).equals(target)) {
                found.set(true);
                return new MiningResultBlock(nonce, hash, threadId, attempts);
            }

            if (attempts % 10000 == 0 && found.get()) {
                return null;
            }
        }

        return null;
    }

}