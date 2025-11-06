package org.graph.infrastructure.provider;

import org.graph.domain.entities.p2p.Node;

import java.math.BigInteger;
import java.util.List;

public interface KademliaIController {
    List<Node> findNode(BigInteger nodeId);
    Object findValue(BigInteger hash);
    boolean ping(Node node);
    void storage(BigInteger nodeId, Object value);
}