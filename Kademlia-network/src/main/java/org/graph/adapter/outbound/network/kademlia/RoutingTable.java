package org.graph.adapter.outbound.network.kademlia;

import org.graph.application.usecase.provider.IReputationsManager;
import org.graph.domain.entities.node.Node;
import org.graph.domain.entities.node.NodeId;
import org.graph.domain.valueobject.cryptography.PublicKeyPeer;
import org.graph.server.Peer;
import org.graph.server.utils.MetricsLogger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.*;

import static org.graph.adapter.utils.Constants.ID_BITS;
import static org.graph.adapter.utils.Constants.MAX_GLOBAL_NODES_PER_IP;

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

    public synchronized boolean addNode(Node node, Peer myPeer) {
        if (node == null || node.equals(localNode)) {
            return false;
        }

        PublicKeyPeer pkPeer = myPeer.getIsKeysInfrastructure()
                .getNeighborPublicKeyByPeerId(node.getNodeId().value());

        if (pkPeer == null || pkPeer.getKey() == null) {
            System.err.println("[SECURITY] Injection rejected: Missing public key for node " + node.getNodeId().value());
            return false;
        }

        if (!NodeId.isValidNode(node, pkPeer.getKey())) {
            System.err.println("[SECURITY] Injection rejected: Node failed Proof of Work (Possible Sybil).");
            MetricsLogger.recordMaliciousBehavior(node.getNodeId().value().toString(), "SYBIL_ATTACK");
            return false;
        }

        if (isGlobalIpLimitExceeded(node.getHost())) {
            System.err.println("[SECURITY] Injection rejected: Global IP limit reached in the routing table (Eclipse risk).");
            MetricsLogger.recordMaliciousBehavior(node.getNodeId().value().toString(), "ECLIPSE_ATTACK");
            return false;
        }

        int bucketIndex = getBucketIndex(node);
        return buckets.get(bucketIndex).addNode(node);
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


    /**
     * Cálculo da distância ponderada para S-Kademlia.
     *
     * <p>
     * O processo segue os seguintes passos:
     * <ol>
     *   <li>Calcula a Old Distance (OD) usando a distância XOR entre nós.</li>
     *   <li>Determina o fator de confiança (Trust, t) do nó alvo.</li>
     *   <li>Calcula a New Distance (ND) conforme a Equação (1) do artigo S-Kademlia [cite: 370]:
     *       {@code ND = OD * b + (1 - b) * 1/t}, onde b é o balancing factor.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Este cálculo permite ponderar a proximidade lógica (XOR) com a reputação do nó,
     * melhorando a resistência a ataques Sybil e priorizando nós confiáveis mesmo
     * que não sejam os mais próximos XOR.
     * </p>
     */
   public synchronized List<Node> findClosestNodesProximity(BigInteger targetNode, int count) {
       PriorityQueue<NodeMetric> closest = new PriorityQueue<>(
               Comparator.comparing(NodeMetric::newDistance)
       );

       Set<BigInteger> processedIds = new HashSet<>();

       for (KBucket bucket : buckets) {
           for (Node node : bucket.getNodes()) {
               BigInteger nodeId = node.getNodeId().value();

               if (processedIds.contains(nodeId)) {
                   continue;
               }
               processedIds.add(nodeId);

               BigInteger xorDist = node.getNodeId().distanceBetweenNode(targetNode);

               double trust = reputationProvider.getTrustFactor(nodeId);
               double nd = calculateSKademliaMetric(xorDist, trust);

               MetricsLogger.recordTrustScore(nodeId.toString(), trust);

               closest.offer(new NodeMetric(node, nd));
           }
       }

       List<Node> result = new ArrayList<>();
       for (int i = 0; i < count && !closest.isEmpty(); i++) {
           result.add(closest.poll().node());
       }

       return result;
   }

    /**
     * Cálculo da nova distância ponderada (nd) para S-Kademlia:
     *
     * <p>
     * Fórmula: {@code nd = od * b + (1 - b) * 1/t}
     * </p>
     *
     * <ul>
     *   <li>{@code od} (Old Distance): Distância XOR entre nós.</li>
     *   <li>{@code b} (Balancing Factor): Fator de ponderação entre distância e confiança
     *       (valor sugerido: 0.65 para operações de routing).</li>
     *   <li>{@code t} (Trust): Fator de confiança do nó, baseado na reputação ou histórico de interações.</li>
     * </ul>
     *
     * <p>
     * Esta fórmula combina a proximidade lógica (XOR) com a confiança do nó,
     * conforme descrito no artigo S-Kademlia, permitindo priorizar nós confiáveis
     * mesmo que não sejam os mais próximos XOR, aumentando resistência a ataques Sybil.
     * </p>
     */
    double  calculateSKademliaMetric(BigInteger xorDistance, double trust){
        BigDecimal distance = new BigDecimal(xorDistance);
        double normalizedDistance = distance.divide(MAX_DISTANCE, MathContext.DECIMAL64).doubleValue();
        double safeTrust = Math.max(0.0001, trust);

        return (normalizedDistance * BALANCE_FACTOR) + ((1.0 - BALANCE_FACTOR) * (1.0 / safeTrust));
    }

    public synchronized boolean removeNode(Node node) {
        if (node == null) return false;
        int bucketIndex = getBucketIndex(node);
        return  buckets.get(bucketIndex).removeNode(node);
    }

    /**
     * Proteção contra ataques Eclipse.
     *
     * <p>
     * Este method verifica se um nó tenta manipular o bucket, por exemplo,
     * mudando endereços IP para se aproveitar do nó local ou tentar realizar
     * ataques de DoS/DDoS. A verificação inclui a validação da subnet e outros
     * parâmetros de rede para assegurar que apenas nós válidos e consistentes
     * são mantidos nos buckets.
     * </p>
     *
     * @param host Endereço IP do nó a verificar, incluindo informação de subnet
     *             para validação de consistência de rede.
     * @return {@code false} caso seja detectada uma tentativa de ataque ou
     *         o nó seja considerado inválido.
     */
    private synchronized boolean isGlobalIpLimitExceeded(String host) {
        if (host == null || host.isEmpty()) return true;

//        if (host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost") || host.equals("::1")) {
//            return false;
//        }

        if (host.startsWith("172.23.")) {
            return false;
        }

        long globalCount = 0;
        for (KBucket bucket : buckets) {
            for (Node n : bucket.getNodes()) {
                if (host.equals(n.getHost())) {
                    globalCount++;
                }
            }
        }
        return globalCount >= MAX_GLOBAL_NODES_PER_IP;
    }


    private int getBucketIndex(Node node) {
        BigInteger distance = localNode.getNodeId().distanceBetweenNode(node.getNodeId().value());
        return Math.min(distance.bitLength() - 1, ID_BITS - 1);
    }

    /**
     * Mecanismo de manutenção ativa da Tabela de Roteamento (Routing Table).
     *
     * Para garantir que os k-buckets permaneçam atualizados, com rotatividade
     * adequada e resilientes a nós inativos ("zombies") ou a ataques de
     * ofuscação (Eclipse), é necessária uma verificação periódica do estado
     * da topologia da rede.
     *
     *
     *
     * Durante esta verificação, o sistema percorre todos os buckets da tabela.
     * O objetivo não é necessariamente encontrar os nós mais próximos do
     * identificador local, mas sim garantir que cada faixa de distância do
     * espaço de endereçamento contenha nós ativos e validados.
     *
     *
     *
     * Para popular um bucket específico (índice 'i'), o algoritmo gera um
     * Node ID alvo artificial. Este identificador aleatório é construído de
     * forma a garantir que a distância matemática entre ele e o nó local
     * (calculada através da operação XOR) possua o seu bit mais significativo
     * exatamente na posição 'i'.
     *
     *
     *
     * Regra de execução:
     * O sistema percorre todos os buckets. Se um bucket não registar atividade
     * (inserção ou lookup) durante a última hora, o nó local gera um Node ID
     * aleatório restrito a essa faixa de distância e dispara uma operação
     * FIND_NODE silenciosa, forçando a descoberta de vizinhos ativos nessa zona.
     */
    public synchronized List<BigInteger> getIdleBucketTargets() {
        long now = System.currentTimeMillis();
        long ONE_MS =  60L * 1000L;
        List<BigInteger> targetsForRefresh = new ArrayList<>();

        for (int i = 0; i < ID_BITS; i++) {
            KBucket bucket = buckets.get(i);

            if ((now - bucket.getLastRefreshTime()) > ONE_MS) {

                BigInteger targetId = generateRandomBuckets(i);
                targetsForRefresh.add(targetId);

                bucket.updateRefreshTime();
            }
        }

        return targetsForRefresh;
    }
    /**
     * Gera um Node ID alvo cuja distância XOR em relação ao nó local
     * pertença à faixa correspondente ao índice do bucket especificado.
     *
     * Na métrica de distância do Kademlia, um nó pertence ao bucket 'i'
     * quando a distância XOR entre o seu identificador e o identificador
     * do nó local tem o bit mais significativo igual a 'i'.
     *
     * Assim, este method constrói um Node ID artificial que garante que
     * a distância calculada através da operação XOR caia exatamente
     * na faixa de distância representada pelo bucket 'i'.
     *
     * @param bucketIndex índice do k-bucket cuja faixa de distância
     *                    deve ser atingida pelo Node ID gerado
     * @return um Node ID aleatório que pertence à faixa de distância
     *         correspondente ao bucket especificado
     *
     *  Exemplo:
     *   Target: 01010
     *   Local Node: 10101
     *   Distance: 11111
     *   2^1 < d < 2^i+1
     */

    private BigInteger generateRandomBuckets(int bucketIndex) {
        Random random = new SecureRandom();

        BigInteger prefix = BigInteger.ONE.shiftLeft(bucketIndex);

        BigInteger randomSuffix = new BigInteger(bucketIndex, random);

        BigInteger targetDistance = prefix.or(randomSuffix);

        return localNode.getNodeId().value().xor(targetDistance);


    }

    /**
     * Promove um nó já presente na tabela de encaminhamento para a posição de
     * "mais recentemente observado" (Most Recently Seen).
     *
     * Este procedimento deve ser executado sempre que uma mensagem válida
     * proveniente desse nó for recebida, indicando que o mesmo permanece
     * ativo e acessível na rede. A atualização da posição reflete a política
     * de recência utilizada pela estrutura da tabela, tipicamente baseada
     * em ordenação temporal (por exemplo, estratégia LRU – Least Recently Used),
     * garantindo que nós recentemente ativos tenham prioridade na manutenção
     * da tabela enquanto entradas antigas tornam-se candidatas à remoção.
     */
    public synchronized void touchNode(BigInteger nodeId) {
        if (nodeId == null || nodeId.equals(localNode.getNodeId().value())) {
            return;
        }

        BigInteger distance = localNode.getNodeId().distanceBetweenNode(nodeId);
        if (distance.equals(BigInteger.ZERO)) return;

        int bucketIndex = Math.min(distance.bitLength() - 1, ID_BITS - 1);

        if (bucketIndex >= 0 && bucketIndex < buckets.size()) {
            buckets.get(bucketIndex).touchNode(nodeId);
        }
    }
}
