package org.graph.domain.application.mechanism.pow;

import java.math.BigInteger;


public class MiningResult {
    private final long nonce;
    private final BigInteger nodeId;
    private final int threadId;

    public MiningResult(long nonce, BigInteger nodeId, int threadId) {
        this.nonce = nonce;
        this.nodeId = nodeId;
        this.threadId = threadId;
    }

    public long getNonce() { return nonce; }
    public BigInteger getNodeId() { return nodeId; }
}