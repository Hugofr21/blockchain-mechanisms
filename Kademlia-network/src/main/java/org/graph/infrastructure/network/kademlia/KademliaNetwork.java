package org.graph.infrastructure.network.kademlia;

import org.graph.domain.application.kademlia.RoutingTable;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.p2p.Peer;
import org.graph.infrastructure.provider.KademliaIController;

import java.math.BigInteger;
import java.util.List;

public class KademliaNetwork implements KademliaIController {
    private Peer myself;

    public KademliaNetwork(Peer myself) {
        this.myself = myself;
    }


    @Override
    public List<Node> findNode(BigInteger nodeId) {
        return null;
    }

    @Override
    public Object findValue(BigInteger hash) {
        return null;
    }

    @Override
    public boolean ping(Node node) {
        RoutingTable routingTable = myself.getRoutingTable();
        List<Node> targetNode =  routingTable.findClosestNodes(node, 1);

        return false;
    }

    @Override
    public void storage(BigInteger nodeId, Object value) {return;}

}
