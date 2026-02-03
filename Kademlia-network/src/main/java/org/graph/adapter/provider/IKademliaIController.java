package org.graph.adapter.provider;

import org.graph.domain.entities.node.Node;

import java.math.BigInteger;
import java.util.List;

public interface IKademliaIController {
    List<Node>  findNode(BigInteger nodeId);
    <T> T findValue(BigInteger key, Class<T> type);
    boolean ping(Node node);
    void storage(BigInteger nodeId, Object value);
}