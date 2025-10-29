package org.graph.network.provider;

import org.graph.p2p.Node;

import java.math.BigInteger;
import java.util.LinkedHashMap;

public class KBucket {
    private LinkedHashMap<BigInteger, Node> nodes;


    public boolean addNode(Node node) {
        return false;
    }

    public Node getClosetNode() {
        return null;
    }

    public boolean removeNode(Node node) {
        return false;
    }

}
