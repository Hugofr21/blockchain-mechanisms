package org.graph.domain.application.mining;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;


public record MinerThread(
        int threadId,
        long startNonce,
        long nonceRange,
        byte[] rawPublicKey,
        int difficulty,
        AtomicBoolean found
) implements Callable<MiningResult> {


    @Override
    public MiningResult call() throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(rawPublicKey.length + Long.BYTES);
            buffer.put(rawPublicKey);

            long endNonce = startNonce + nonceRange;
            BigInteger target = BigInteger.ONE.shiftLeft(256 - difficulty);

            for (long nonce = startNonce; nonce < endNonce; nonce++) {

                if (found.get()) {
                    throw new RuntimeException("Worker stopped: Solution found by another thread");
                }


                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                buffer.putLong(rawPublicKey.length, nonce);
                digest.update(buffer.array());
                byte[] hashBytes = digest.digest();


                if ((hashBytes[0] & 0xFF) == 0) {
                    BigInteger idCandidate = new BigInteger(1, hashBytes);
                    if (idCandidate.compareTo(target) < 0) {

                        if (found.compareAndSet(false, true)) {
                            return new MiningResult(nonce, idCandidate, threadId);
                        } else {
                            throw new RuntimeException("Race condition lost");
                        }
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }


        throw new RuntimeException("Nonce not found in this range");
    }
}