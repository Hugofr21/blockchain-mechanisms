package org.graph.domain.application.kademlia;

import org.graph.domain.entities.p2p.Node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import static org.graph.adapter.utils.Constants.ID_BITS;

public class RoutingTable {
    private List<KBucket> buckets;
    private Node localNode;

    private static final double BALANCE_FACTOR = 0.65;

    // Constante para normalizar a distância XOR (2^256) para o intervalo [0, 1]
    private static final BigDecimal MAX_DISTANCE = new BigDecimal(BigInteger.ONE.shiftLeft(ID_BITS));

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
        BigInteger distance = localNode.getNodeId().distanceBetweenNode(node.getNodeId().value());
        return Math.min(distance.bitLength() - 1, ID_BITS - 1);
    }

    public synchronized Node getByNodeIdNode(BigInteger nodeId){
       for (KBucket bucket : buckets) {
           List<Node>  list = bucket.getNodes();
           for (Node node : list) {
               if (nodeId.equals(node.getNodeId())) {
                   return node;
               }
           }
       }
       return null;
    }

   /*
      // 1. Calcula Old Distance (OD) - XOR
      // 2. Calcula Trust (t)
      // 3. Calcula New Distance (ND) conforme Eq. (1) do artigo [cite: 370]
    */
    public synchronized List<Node> findClosestNodesProximity(BigInteger targetNode, int count) {
        PriorityQueue<NodeMetric> closest = new PriorityQueue<>(
                Comparator.comparing(NodeMetric::newDistance)
        );

        for (KBucket bucket : buckets) {
            for (Node node : bucket.getNodes()) {

                BigInteger xorDist = node.getNodeId().distanceBetweenNode(targetNode);


                double trust = node.getMyProofOfReputation().getTrustFactor();

                double nd = calculateSKademliaMetric(xorDist, trust);

                closest.offer(new NodeMetric(node, nd));
            }
        }

        List<Node> result = new ArrayList<>();
        for (int i = 0; i < count && !closest.isEmpty(); i++) {
            result.add(closest.poll().node());
        }
        return result;
    }

    // nd = od * b + (1-b) * 1/t
    // Onde:od (Old Distance): Distância XOR.
    // b (Balancing Factor): Fator de peso (0.65 sugerido para routing)
    // t (Trust): Fator de confiança.
    private double  calculateSKademliaMetric(BigInteger xorDistance, double trust ){
        BigDecimal distance = new BigDecimal(xorDistance);
        double normalizedDistance = distance.divide(MAX_DISTANCE, MathContext.DECIMAL64).doubleValue();

        double safeTrust = Math.max(0.0001, trust);

        return (normalizedDistance * BALANCE_FACTOR) + ((1.0 - BALANCE_FACTOR) * (1.0 / safeTrust));

    }
}
