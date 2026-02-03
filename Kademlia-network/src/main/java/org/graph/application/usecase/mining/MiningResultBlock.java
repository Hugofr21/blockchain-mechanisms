package org.graph.domain.application.mining;

public record MiningResultBlock(int nonce, String hash, int threadId, long attempts) {
}
