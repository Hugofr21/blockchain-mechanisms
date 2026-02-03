package org.graph.domain.application.mining;

import java.math.BigInteger;


public record MiningResult(long nonce, BigInteger nodeId, int threadId) {
}