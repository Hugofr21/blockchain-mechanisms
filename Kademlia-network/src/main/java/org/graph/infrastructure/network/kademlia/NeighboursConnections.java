package org.graph.infrastructure.network.kademlia;

import org.graph.domain.entities.p2p.Node;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NeighboursConnections{
    private Map<BigInteger, Long> actives;

    public NeighboursConnections() {
        this.actives = new ConcurrentHashMap<>();
    }


    public void sendHeartbeats(Node node) {

    }

    public void receiveHeartbeats(Node node) {

    }
}
