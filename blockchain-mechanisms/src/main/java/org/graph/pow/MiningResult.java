package org.graph.pow;

public class MiningResult {
    private final int nonce;
    private final String hash;
    private final int threadId;
    private final long attempts;

    public MiningResult(int nonce, String hash, int threadId, long attempts) {
        this.nonce = nonce;
        this.hash = hash;
        this.threadId = threadId;
        this.attempts = attempts;
    }

    public int getNonce() { return nonce; }
    public String getHash() { return hash; }
    public int getThreadId() { return threadId; }
    public long getAttempts() { return attempts; }
}
