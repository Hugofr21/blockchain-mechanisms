package org.graph.application.usecase.mining;

public record MiningResultBlock(long nonce, String hash, int threadId, long attempts) {
}
