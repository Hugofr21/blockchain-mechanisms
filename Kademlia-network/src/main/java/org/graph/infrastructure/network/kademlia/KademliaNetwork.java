package org.graph.infrastructure.network.kademlia;

import org.graph.domain.application.kademlia.RoutingTable;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.provider.KademliaIController;

import java.math.BigInteger;
import java.util.List;

public class KademliaNetwork implements KademliaIController {
    private RoutingTable mRoutingTable;

    @Override
    public List<Node> findNode(BigInteger nodeId) {
        return null;
    }

    @Override
    public Object findValue(BigInteger hash) {
        return null;
    }

    @Override
    public boolean ping(BigInteger nodeId) {
        return false;
    }

    @Override
    public void storage(BigInteger nodeId, Object value) {return;}

}
