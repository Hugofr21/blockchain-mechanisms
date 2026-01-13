package org.graph.pow;

public record MiningResultBlock(int nonce, String hash, int threadId, long attempts) {
}
