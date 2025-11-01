package org.graph.infrastructure.network.kademlia;

import org.graph.infrastructure.network.provider.KademliaIController;
import org.graph.domain.entities.p2p.Node;

import java.math.BigInteger;
import java.util.List;

public class KademliaNetwork implements KademliaIController {

    @Override
    public List<Node> findNode(BigInteger nodeId) {
        return List.of();
    }

    @Override
    public Object findValue(BigInteger hash) {
        return null;
    }

    @Override
    public boolean ping(BigInteger nodeId) {return false;}

    @Override
    public void storage(BigInteger nodeId, Object value) {return;}

}
