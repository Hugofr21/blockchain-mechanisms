package org.graph.application.usecase.provider;

import java.math.BigInteger;

public interface IReputationsManager {
    double getTrustFactor(BigInteger nodeId);
}
