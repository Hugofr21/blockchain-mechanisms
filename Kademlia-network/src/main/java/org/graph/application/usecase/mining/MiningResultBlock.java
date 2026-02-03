package org.graph.application.usecase.mining;

public record MiningResultBlock(int nonce, String hash, int threadId, long attempts) {
}
