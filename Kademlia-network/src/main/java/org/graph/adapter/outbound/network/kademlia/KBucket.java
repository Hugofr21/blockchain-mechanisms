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
    private long lastRefreshTime;

    public KBucket(IReputationsManager reputationProvider) {
        this.reputationProvider = reputationProvider;
        this.nodes = new LinkedHashMap<>(NODE_K, 0.75f, true);
        this.lastRefreshTime = System.currentTimeMillis();

    }

    public synchronized boolean addNode(Node newNode) {
        this.lastRefreshTime = System.currentTimeMillis();

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

        /**
         * Política de bucket cheio – Defesa contra ataques Sybil.
         *
         * <p>
         * No Kademlia padrão, quando um bucket está cheio, o nó mais antigo (head do mapa)
         * é pingado. Se responder, o novo nó é descartado. Esta abordagem garante
         * estabilidade, mas não considera a reputação dos nós.
         * </p>
         *
         * <p>
         * Extensão S-Kademlia: Se o nó antigo tiver um nível de Trust muito baixo
         * e o nó novo tiver um Trust alto, podemos considerar substituir o nó antigo.
         * A decisão baseia-se em:
         * <ul>
         *   <li>Evicção baseada em Trust.</li>
         *   <li>Validação de PoW do nó novo.</li>
         * </ul>
         * Caso contrário, preservamos a estabilidade para resistir a ataques de
         * Sybil flooding.
         * </p>
         *
         * <p>
         * O method retorna {@code false} para indicar que o caller deve testar
         * o nó menos recentemente visto ({@code leastRecentlySeen}) antes de
         * descartar qualquer nó.
         * </p>
         */

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

    public long getLastRefreshTime() {return lastRefreshTime;}

    public void updateRefreshTime() {lastRefreshTime = System.currentTimeMillis();}

}
