package org.graph.adapter.outbound.network.kademlia;

import org.graph.application.usecase.provider.IReputationsManager;
import org.graph.domain.entities.node.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static org.graph.adapter.utils.Constants.NODE_K;

public class KBucket { ;
    private LinkedHashMap<BigInteger, Node> nodes;
    private final IReputationsManager reputationProvider;
    public KBucket(IReputationsManager reputationProvider) {
        this.reputationProvider = reputationProvider;
        this.nodes = new LinkedHashMap<>(NODE_K, 0.75f, true);

    }

    public synchronized boolean addNode(Node newNode) {
        BigInteger nodeId = newNode.getNodeId().value();

        if (nodes.containsKey(nodeId)) {
            nodes.remove(nodeId);
            nodes.put(nodeId, newNode);
            return true;
        }

        if (nodes.size() < NODE_K) {
            nodes.put(nodeId, newNode);
            return true;
        }

        // 3. Bucket Cheio - Política de Defesa Sybil
        // O Kademlia padrão tenta pingar o nó mais antigo (head do map).
        // Se o antigo responder, o novo é descartado.
        // S-Kademlia Extension: Se o antigo tiver Trust MUITO baixo e o novo tiver Trust alto,
        // podemos considerar a troca.
        // Lógica S-Kademlia: Evicção baseada em Trust [cite: 58]
        // Se o nó antigo for suspeito (trust baixo) e o novo tiver PoW válido...
        // Caso contrário, preferimos a estabilidade (resistência a Sybil Flooding)
        // Retornamos false indicando que o caller deve testar o 'leastRecentlySeen' antes de descartar

        Node leastRecentlySeen = getLeastRecentlySeen();
        if (leastRecentlySeen != null) {

            double oldTrust = reputationProvider.getTrustFactor(leastRecentlySeen.getNodeId().value());
            double newTrust = reputationProvider.getTrustFactor(nodeId);

            if (oldTrust < 0.5 && newTrust > 1.5) {
                System.out.println("[DEBUG] Replacing weak node (" + oldTrust + ") by strong (" + newTrust + ")");
                nodes.remove(leastRecentlySeen.getNodeId().value());
                nodes.put(nodeId, newNode);
                return true;
            }

            return false;
        }

        return false;
    }
    public synchronized Node getLeastRecentlySeen() {
        Iterator<Node> it = nodes.values().iterator();
        return it.hasNext() ? it.next() : null;
    }

    public synchronized Node getClosestNode() {
        return nodes.values().stream().findFirst().orElse(null);
    }

    public synchronized boolean removeNode(Node node) {
        return nodes.remove(node.getNodeId().value()) != null;
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
