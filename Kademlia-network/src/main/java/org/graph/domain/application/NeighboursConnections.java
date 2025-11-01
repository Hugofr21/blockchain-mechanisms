package org.graph.domain.application;

import org.graph.domain.common.Pair;
import org.graph.domain.entities.p2p.Node;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NeighboursConnections{
    private final Map<BigInteger, Long> lastTimestamp;
    private final Map<BigInteger, Pair<Node, ConnectionHandler>> nodesActives;

    public NeighboursConnections() {
        this.lastTimestamp = new ConcurrentHashMap<>();
        this.nodesActives = new ConcurrentHashMap<>();
    }


    public void sendHeartbeats(Node node) {

    }

    public void receiveHeartbeats(Node node) {

    }
}
