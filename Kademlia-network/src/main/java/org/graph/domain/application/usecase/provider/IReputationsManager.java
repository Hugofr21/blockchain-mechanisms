package org.graph.adapter.provider;

import java.math.BigInteger;

public interface IReputationsManager {
    double getTrustFactor(BigInteger nodeId);
}
