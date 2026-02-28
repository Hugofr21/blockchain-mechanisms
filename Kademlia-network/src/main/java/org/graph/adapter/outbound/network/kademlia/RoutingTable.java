package org.graph.adapter.outbound.network.kademlia;

import org.graph.application.usecase.provider.IReputationsManager;
import org.graph.domain.entities.node.Node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

import static org.graph.adapter.utils.Constants.ID_BITS;

public class RoutingTable {
    private List<KBucket> buckets;
    private Node localNode;
    private final IReputationsManager reputationProvider;
    private static final double BALANCE_FACTOR = 0.65;
    // Constante para normalizar a distância XOR (2^256) para o intervalo [0, 1]
    private static final BigDecimal MAX_DISTANCE = new BigDecimal(BigInteger.ONE.shiftLeft(ID_BITS));

    public RoutingTable(Node localNode , IReputationsManager reputationProvider) {
        this.localNode = localNode;
        this.buckets = new ArrayList<>();
        for (int i = 0; i < ID_BITS; i++) {
            buckets.add(new KBucket(reputationProvider));
        }
        this.reputationProvider = reputationProvider;
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
       // PriorityQueue para ordenar pela métrica S/Kademlia (Distância Ajustada)
       PriorityQueue<NodeMetric> closest = new PriorityQueue<>(
               Comparator.comparing(NodeMetric::newDistance)
       );

       // Set para evitar duplicados (O(1) lookup)
       // Usamos o BigInteger (ID) como chave de unicidade
       Set<BigInteger> processedIds = new HashSet<>();

       for (KBucket bucket : buckets) {
           for (Node node : bucket.getNodes()) {
               BigInteger nodeId = node.getNodeId().value();

               // 1. FILTRAGEM DEFENSIVA: Evita nós duplicados na resposta
               if (processedIds.contains(nodeId)) {
                   continue;
               }
               processedIds.add(nodeId);

               // Evita adicionar-nos a nós mesmos na lista de routing
               // (Assumindo que tens acesso ao myselfId, senão remove este if)
               // if (nodeId.equals(myselfId)) continue;

               // 2. CÁLCULO DA MÉTRICA
               BigInteger xorDist = node.getNodeId().distanceBetweenNode(targetNode);

               // CORREÇÃO CRÍTICA: O Trust Factor deve ser do NÓ VIZINHO, não do ALVO.
               // Se usares targetNode, o trust é constante para todos no loop, anulando a eq. (1).
               double trust = reputationProvider.getTrustFactor(nodeId);

               // 3. Aplica a Eq. (1) do S/Kademlia
               double nd = calculateSKademliaMetric(xorDist, trust);

               closest.offer(new NodeMetric(node, nd));
           }
       }

       // Extração dos Top-K resultados
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

    public synchronized boolean removeNode(Node node) {
        if (node == null) return false;

        int bucketIndex = getBucketIndex(node);

        boolean removed = buckets.get(bucketIndex).removeNode(node);

        if (removed) {
            System.out.println("[ROUTING] Nó removido da tabela: " + node.getNodeId());
        }
        return removed;
    }
}
