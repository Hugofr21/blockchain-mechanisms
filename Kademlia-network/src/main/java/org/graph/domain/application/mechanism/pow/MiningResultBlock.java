package org.graph.domain.application.mechanism.pow;

public record MiningResultBlock(int nonce, String hash, int threadId, long attempts) {
}
