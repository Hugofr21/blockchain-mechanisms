package org.graph.domain.application.kademlia;

import org.graph.domain.entities.p2p.Node;
import org.graph.domain.entities.p2p.NodeId;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static org.graph.infrastructure.utils.Constants.ID_BITS;

public class RoutingTable {
    private List<KBucket> buckets;
    private Node localNode;

    public RoutingTable(Node localNode) {
        this.localNode = localNode;
        this.buckets = new ArrayList<>();
        for (int i = 0; i < ID_BITS; i++) {
            buckets.add(new KBucket());
        }
    }

    public synchronized boolean addNode(Node node) {
        if (node.equals(localNode)) return false;

        int bucketIndex = getBucketIndex(node);
        return buckets.get(bucketIndex).addNode(node);
    }

    private int getBucketIndex(Node node) {
        BigInteger distance = localNode.getNodeId().distanceBetween(node.getNodeId());
        return Math.min(distance.bitLength() - 1, ID_BITS - 1);
    }

    public synchronized List<Node> findClosestNodes(Node targetNode, int count) {
        PriorityQueue<NodeDistance> closest = new PriorityQueue<>(
                Comparator.comparing(NodeDistance::getDistance)
        );

        for (KBucket bucket : buckets) {
            for (Node node : bucket.getNodes()) {
                BigInteger distance = node.getNodeId().distanceBetween(
                        targetNode.getNodeId()
                );
                closest.offer(new NodeDistance(node, distance));
            }
        }

        List<Node> result = new ArrayList<>();
        for (int i = 0; i < count && !closest.isEmpty(); i++) {
            result.add(closest.poll().getNode());
        }

        return result;
    }

    private static class NodeDistance {
        private Node node;
        private BigInteger distance;

        public NodeDistance(Node node, BigInteger distance) {
            this.node = node;
            this.distance = distance;
        }

        public Node getNode() { return node; }
        public BigInteger getDistance() { return distance; }
    }

}
