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
         * Política aplicada quando um K-Bucket atinge a sua capacidade máxima,
         * incorporando mecanismos adicionais de mitigação contra ataques Sybil.
         *
         * <p>
         * No algoritmo Kademlia original, quando um bucket está cheio, o nó menos
         * recentemente observado (head da estrutura ordenada por LRU) é submetido
         * a um teste de liveness através de uma mensagem de ping. Caso esse nó
         * responda, assume-se que continua operacional e o novo nó candidato é
         * simplesmente descartado. Este comportamento favorece a estabilidade
         * estrutural da tabela de encaminhamento, privilegiando nós com histórico
         * prolongado de disponibilidade. Contudo, o modelo clássico não incorpora
         * qualquer noção de reputação ou confiabilidade.
         * </p>
         *
         * <p>
         * A variante S-Kademlia introduz um critério adicional baseado em confiança
         * (Trust). Caso o nó mais antigo apresente um nível de confiança inferior a
         * um limiar definido e o novo nó apresente um valor de Trust significativamente
         * superior, o sistema pode considerar a substituição do nó antigo. Esta
         * decisão não é tomada de forma automática, sendo condicionada por dois
         * fatores principais:
         * </p>
         *
         * <ul>
         *   <li>Avaliação comparativa de Trust entre o nó existente e o nó candidato,
         *   permitindo evicção seletiva de nós potencialmente maliciosos ou pouco
         *   confiáveis.</li>
         *   <li>Validação do mecanismo de Proof-of-Work (PoW) apresentado pelo nó
         *   candidato, utilizado como barreira computacional para limitar a criação
         *   massiva de identidades Sybil.</li>
         * </ul>
         *
         * <p>
         * Na ausência destas condições, mantém-se o comportamento conservador do
         * Kademlia original, preservando nós estáveis na tabela e reduzindo a
         * probabilidade de ataques de flooding baseados na inserção massiva de
         * identidades controladas por um adversário.
         * </p>
         *
         * <p>
         * Este método retorna {@code false} para sinalizar que a decisão final ainda
         * depende da verificação de disponibilidade do nó menos recentemente visto
         * ({@code leastRecentlySeen}). O código chamador deverá executar o teste de
         * conectividade antes de descartar qualquer entrada existente no bucket.
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

    /**
     * Atualiza a posição de um nó dentro do K-Bucket segundo a política de
     * recência (LRU – Least Recently Used) e renova o instante temporal de
     * referência para operações de refresh.
     *
     * A implementação explora o comportamento interno de um LinkedHashMap
     * configurado com o parâmetro accessOrder=true, no qual qualquer acesso
     * a uma entrada provoca automaticamente a sua deslocação para o final da
     * estrutura de iteração. Este mecanismo preserva a ordenação baseada no
     * momento do último acesso, permitindo que nós recentemente contactados
     * sejam mantidos como mais ativos enquanto entradas menos utilizadas
     * permanecem nas posições iniciais e tornam-se candidatas naturais à
     * substituição quando a capacidade do K-Bucket é atingida.
     */
    public synchronized boolean touchNode(BigInteger nodeId) {
        if (nodes.containsKey(nodeId)) {
            nodes.get(nodeId);
            this.updateRefreshTime();
            return true;
        }
        return false;
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

    public Object getActiveNodesCount() {
        return nodes.size();
    }
}
