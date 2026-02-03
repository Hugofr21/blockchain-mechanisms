package org.graph.domain.application.usecase.provider;

import java.math.BigInteger;

public interface IReputationsManager {
    double getTrustFactor(BigInteger nodeId);
}
