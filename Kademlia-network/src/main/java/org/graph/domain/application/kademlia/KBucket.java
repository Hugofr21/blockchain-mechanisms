package org.graph.domain.application.kademlia;

import org.graph.domain.entities.p2p.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.graph.infrastructure.utils.Constants.NODE_K;

public class KBucket { ;
    private LinkedHashMap<BigInteger, Node> nodes;

    public KBucket() {
        this.nodes = new LinkedHashMap<>(NODE_K, 0.75f, true);
    }

    public synchronized boolean addNode(Node node) {
        BigInteger nodeId = node.getNodeId().getId();

        if (nodes.containsKey(nodeId)) {
            nodes.remove(nodeId);
            nodes.put(nodeId, node);
            return true;
        }

        if (nodes.size() < NODE_K) {
            nodes.put(nodeId, node);
            return true;
        }

        return false;
    }

    public synchronized Node getClosestNode() {
        return nodes.values().stream().findFirst().orElse(null);
    }

    public synchronized boolean removeNode(Node node) {
        return nodes.remove(node.getNodeId().getId()) != null;
    }

    public synchronized List<Node> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    public synchronized int size() {
        return nodes.size();
    }

    public synchronized boolean isFull() {
        return nodes.size() >= NODE_K;
    }

}
