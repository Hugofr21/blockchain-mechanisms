package org.graph.adapter.provider;

import org.graph.domain.entities.p2p.Node;

import java.math.BigInteger;
import java.util.List;

public interface KademliaIController {
    public  List<Node>  findNode(BigInteger nodeId);
    public Object findValue(BigInteger hash);
    public boolean ping(Node node);
    public void storage(BigInteger nodeId, Object value);
}