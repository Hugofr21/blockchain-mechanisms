package org.graph.domain.application.mechanism.pow;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;


public class MinerThread implements Callable<MiningResult> {
    private final int threadId;
    private final long startNonce;
    private final long nonceRange;
    private final byte[] rawPublicKey;
    private final int difficulty;
    private final AtomicBoolean found;

    public MinerThread(int threadId, long startNonce, long nonceRange,
                       PublicKey publicKey, int difficulty, AtomicBoolean found) {
        this.threadId = threadId;
        this.startNonce = startNonce;
        this.nonceRange = nonceRange;
        this.rawPublicKey = publicKey.getEncoded();
        this.difficulty = difficulty;
        this.found = found;
    }

    @Override
    public MiningResult call() throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(rawPublicKey.length + Long.BYTES);
            buffer.put(rawPublicKey);

            long endNonce = startNonce + nonceRange;
            BigInteger target = BigInteger.ONE.shiftLeft(256 - difficulty);

            for (long nonce = startNonce; nonce < endNonce; nonce++) {
                // CORREÇÃO CRÍTICA:
                // Se a flag 'found' for true, significa que OUTRA thread achou.
                // Esta thread deve falhar (lançar exceção) para ser ignorada pelo invokeAny.
                if (found.get()) {
                    throw new RuntimeException("Worker stopped: Solution found by another thread");
                }

                // Verifica thread interruption (boa prática em Executors)
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                buffer.putLong(rawPublicKey.length, nonce);
                digest.update(buffer.array());
                byte[] hashBytes = digest.digest();

                // Conversão otimizada: checar byte líder antes de criar BigInteger
                // (Otimização opcional, mas recomendada para performance)
                if ((hashBytes[0] & 0xFF) == 0) {
                    BigInteger idCandidate = new BigInteger(1, hashBytes);
                    if (idCandidate.compareTo(target) < 0) {
                        // Tenta ser a primeira a setar true
                        if (found.compareAndSet(false, true)) {
                            return new MiningResult(nonce, idCandidate, threadId);
                        } else {
                            // Perdeu a corrida no último milissegundo
                            throw new RuntimeException("Race condition lost");
                        }
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }

        // Se terminou o range e não achou, lança exceção para o invokeAny continuar procurando
        throw new RuntimeException("Nonce not found in this range");
    }
}