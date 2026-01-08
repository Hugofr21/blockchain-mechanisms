package org.graph.infrastructure.network.kademlia;

import org.graph.domain.application.kademlia.RoutingTable;
import org.graph.domain.entities.p2p.Node;
import org.graph.domain.entities.p2p.NodeId;
import org.graph.infrastructure.p2p.Peer;
import org.graph.infrastructure.provider.KademliaIController;

import java.math.BigInteger;
import java.util.List;

import static org.graph.infrastructure.utils.Constants.NODE_K;

public class KademliaNetwork implements KademliaIController {
    private Peer myself;

    public KademliaNetwork(Peer myself) {
        this.myself = myself;
    }


    @Override
    public Node findNode(BigInteger nodeId) {
        Node target = myself.getRoutingTable().getByNodeIdNode(nodeId);
        if (target != null) {
            return target;
        }

        boolean foundCloser ;
        do {
            NodeId targetNodeId = new NodeId(nodeId);
          List<Node> neighbour = myself.getRoutingTable().findClosestNodesProximity(targetNodeId, NODE_K);
        }while (foundCloser ){

        }
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
    public void storage(BigInteger key, Object value) {

        List<Node> closestNodes = findNode(key);
    }

}
