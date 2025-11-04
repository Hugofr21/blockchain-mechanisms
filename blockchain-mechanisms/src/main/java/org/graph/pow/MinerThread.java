package org.graph.pow;

import org.graph.utils.HashUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinerThread implements Callable<MiningResult> {
    private final int threadId;
    private final int startNonce;
    private final int nonceRange;
    private final String data;
    private final int difficulty;
    private final AtomicBoolean found;

    public MinerThread(int threadId, int startNonce, int nonceRange,
                       String data, int difficulty, AtomicBoolean found) {

        if (difficulty < 1) {
            throw new IllegalArgumentException("difficulty must be >= 1 (hex digits).");
        }

        this.threadId = threadId;
        this.startNonce = startNonce;
        this.nonceRange = nonceRange;
        this.data = data;
        this.difficulty = difficulty;
        this.found = found;
    }

    @Override
    public MiningResult call() {
        String target = new String(new char[difficulty]).replace('\0', '0');
        int endNonce = startNonce + nonceRange;
        long attempts = 0;

        for (int nonce = startNonce; nonce < endNonce && !found.get(); nonce++) {
            attempts++;
            String hash = HashUtils.calculateSha256(data + nonce);

            if (hash.substring(0, difficulty).equals(target)) {
                found.set(true);
                return new MiningResult(nonce, hash, threadId, attempts);
            }

            if (attempts % 10000 == 0 && found.get()) {
                return null;
            }
        }

        return null;
    }

}