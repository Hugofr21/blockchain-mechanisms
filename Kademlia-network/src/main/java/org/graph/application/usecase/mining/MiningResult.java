package org.graph.application.usecase.mining;

import java.math.BigInteger;


public record MiningResult(long nonce, BigInteger nodeId, int threadId) {
}