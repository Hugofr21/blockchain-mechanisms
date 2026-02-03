package org.graph.domain.application.mechanism.pow;

import java.math.BigInteger;


public record MiningResult(long nonce, BigInteger nodeId, int threadId) {
}