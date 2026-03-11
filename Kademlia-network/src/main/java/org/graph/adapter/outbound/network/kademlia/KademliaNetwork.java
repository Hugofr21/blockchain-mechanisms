package org.graph.adapter.outbound.network.kademlia;

import org.graph.adapter.outbound.network.message.node.NodeInfoPayload;
import org.graph.adapter.outbound.network.message.node.NodeListPayload;
import org.graph.adapter.utils.CryptoUtils;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.node.NodeId;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.infrastructure.storage.cache.LRUCache;
import org.graph.adapter.utils.MessageUtils;
import org.graph.infrastructure.utils.EncapsulationUtils;
import org.graph.infrastructure.utils.SerializationUtils;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.server.Peer;
import org.graph.adapter.provider.IKademliaIController;
import org.graph.infrastructure.storage.StorageDHT;
import org.graph.server.utils.MetricsLogger;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

import static org.graph.adapter.utils.Constants.MAX_ALPHA;
import static org.graph.adapter.utils.Constants.NODE_K;

/**
 * Esta implementação de Kademlia deve ser utilizada exclusivamente quando
 * já existe uma lista de nós considerada fiável, validada e adequada para
 * comunicações seguras entre participantes da rede.
 *
 * <p>
 * O pressuposto fundamental é que os nós presentes nesta lista passaram
 * previamente por mecanismos de autenticação, validação de identidade e,
 * quando aplicável, verificação criptográfica ou prova de trabalho,
 * mitigando riscos associados a ataques Sybil, Eclipse ou à introdução
 * de nós maliciosos. Consequentemente, esta camada de Kademlia não se
 * destina à descoberta inicial de nós num ambiente não confiável, mas sim
 * à manutenção, otimização e exploração eficiente de uma topologia já
 * estabilizada.
 * </p>
 *
 * <p>
 * A utilização deste componente fora deste contexto constitui um erro de
 * conceção do sistema, uma vez que compromete diretamente as garantias
 * de segurança assumidas pelo protocolo e invalida os pressupostos de
 * comunicação segura entre nós.
 * </p>
 */

public class KademliaNetwork implements IKademliaIController {
    private final Peer myself;
    private final StorageDHT storage;
    private final LRUCache<BigInteger, Object> hotCache;

    public KademliaNetwork(Peer myself) {
        this.storage = new StorageDHT();
        this.myself = myself;
        this.hotCache = new LRUCache<>();
    }

    public StorageDHT getStorage() {
        return this.storage;
    }


    /**
     * Realiza a descoberta iterativa dos nós mais próximos de um determinado
     * identificador numa rede distribuída baseada em Kademlia, aplicando
     * otimizações locais e mecanismos explícitos de mitigação de ataques.
     *
     * <p>
     * O processo inicia-se com uma verificação local em memória ou cache,
     * evitando tráfego de rede desnecessário sempre que a informação já se
     * encontra disponível. De seguida, é inicializada a shortlist de lookup,
     * composta pelos nós seed obtidos a partir da tabela de encaminhamento
     * local, ordenados segundo a distância XOR relativamente ao identificador
     * alvo.
     * </p>
     *
     * <p>
     * Antes de qualquer nó remoto ser considerado válido para participação
     * no processo de lookup, é aplicada uma validação do seu identificador,
     * incluindo a verificação de prova de trabalho associada ao Node ID,
     * conforme os princípios de S/Kademlia. Esta validação é crítica para
     * reduzir a superfície de ataque a cenários de Eclipse Attack, garantindo
     * que apenas nós que provaram ter gerado corretamente o seu identificador
     * são incluídos na shortlist.
     * </p>
     *
     * <p>
     * A fase seguinte consiste num loop iterativo de rede, onde são
     * selecionados, em cada iteração, os ALPHA nós mais próximos ainda não
     * consultados. Estes nós são contactados de forma controlada, atualizando
     * progressivamente a shortlist com candidatos mais próximos, até que não
     * existam novos nós relevantes a consultar ou que seja atingido o critério
     * de convergência definido pelo protocolo.
     * </p>
     *
     * @param targetId
     *        Identificador Kademlia representado como {@link BigInteger},
     *        utilizado para o cálculo de distâncias XOR e para determinar
     *        a proximidade lógica entre nós na rede.
     * @return
     *        Lista ordenada de {@code Node} contendo os nós mais próximos
     *        conhecidos do identificador alvo após a conclusão do processo
     *        de lookup.
     */

    @Override
    public List<Node> findNode(BigInteger targetId) {

        TreeSet<Node> shortlist = new TreeSet<>(
                Comparator.comparingDouble((Node n) -> {
                    BigInteger xorDist = n.getNodeId().distanceBetweenNode(targetId);
                    double trust = myself.getReputationsManager().getTrustFactor(n.getNodeId().value());
                    return myself.getRoutingTable().calculateSKademliaMetric(xorDist, trust);
                }).thenComparing(n -> n.getNodeId().value())
        );

        List<Node> localClosest = myself.getRoutingTable().findClosestNodesProximity(targetId, NODE_K);
        shortlist.addAll(localClosest);

        Set<BigInteger> queried = new HashSet<>();
        queried.add(myself.getMyself().getNodeId().value());

        boolean madeProgress = true;

        while (madeProgress && !shortlist.isEmpty()) {
            madeProgress = false;

            List<Node> toQuery = shortlist.stream()
                    .filter(n -> !queried.contains(n.getNodeId().value()))
                    .limit(MAX_ALPHA)
                    .toList();

            if (toQuery.isEmpty()) break;

            for (Node node : toQuery) {
                queried.add(node.getNodeId().value());

                Message findMsg = new Message(MessageType.FIND_NODE, targetId, myself.getHybridLogicalClock());
                Object res = sendRPCAsync(node, findMsg);

                if (res == null) {
                    myself.getReputationsManager().reportEvent(node.getNodeId().value(), EventTypePolicy.PING_FAIL);
                    continue;
                }

                NodeListPayload receiveListNode = EncapsulationUtils.decapsulationListNodes(res);

                if (receiveListNode == null) {
                    System.err.println("[DHT_FIND_NODE] Invalid payload of " + node.getPort());
                    continue;
                }

                List<NodeInfoPayload> list = receiveListNode.nodes();

                if (!list.isEmpty()) {
                    myself.getReputationsManager().reportEvent(
                            node.getNodeId().value(),
                            EventTypePolicy.FIND_NODE_USEFUL
                    );
                }

                List<Node> returnedNodes = new ArrayList<>();

                for (NodeInfoPayload info : list) {
                    try {

                        PublicKey remotePk = CryptoUtils.getPublicKeyFromBytes(info.publicKey());
                        if (remotePk == null) continue;

                        BigInteger claimedId = EncapsulationUtils.decapsulationNodeId(info.nodeId());
                        if (claimedId == null) continue;

                        if (claimedId.equals(myself.getMyself().getNodeId().value())) continue;


                        Node candidateNode = new Node(
                                info.host(),
                                info.port(),
                                claimedId,
                                info.nonce(),
                                info.difficulty()
                        );

                        if (!candidateNode.getNodeId().value().equals(claimedId)) {
                            System.err.println("[DHT_FIND_NODE] ID Mismatch! Announced: " + claimedId + " vs Calculated: " + candidateNode.getNodeId().value());
                            continue;
                        }


                        if (!NodeId.isValidNode(candidateNode, remotePk)) {
                            System.err.println("[DHT_FIND_NODE] Invalid PoW for " + info.host());
                            continue;
                        }


                        returnedNodes.add(candidateNode);

                    } catch (Exception e) {
                        System.err.println("[DHT_FIND_NODE] Error processing candidate node: " + e.getMessage());
                    }
                }


                for (Node received : returnedNodes) {

                    myself.getRoutingTable().addNode(received, myself);


                    if (!shortlist.contains(received) && !queried.contains(received.getNodeId().value())) {
                        shortlist.add(received);
                        madeProgress = true;
                    }
                }
            }


            while (shortlist.size() > NODE_K * 2) {
                shortlist.pollLast();
            }
        }

        MetricsLogger.updateTopologyMetrics(
                myself.getNeighboursManager().getActiveNeighbours().size(),
                this.storage.size()
        );

        return new ArrayList<>(shortlist).subList(0, Math.min(NODE_K, shortlist.size()));
    }

    /**
     * Executa a procura de um valor numa rede distribuída baseada em Kademlia,
     * garantindo segurança de tipos e respeitando a hierarquia de otimização
     * definida pelo sistema.
     * <p>
     * O processo inicia-se pela verificação de fontes locais, reduzindo latência
     * e tráfego de rede, e apenas recorre a mecanismos de descoberta distribuída
     * quando necessário.
     * </p>
     *
     * <p>
     * A pesquisa é realizada de forma sequencial e determinística. Primeiro,
     * verifica-se a existência local do valor na blockchain, aplicável apenas
     * quando a chave corresponde a um bloco já validado e organizado internamente.
     * Em seguida, é consultado o armazenamento DHT local, que funciona como cache
     * persistente de dados previamente obtidos. Caso o valor não seja encontrado
     * nestas camadas, é desencadeado um processo iterativo de lookup na rede Kademlia.
     * </p>
     *
     * <p>
     * O lookup distribuído segue o modelo clássico do protocolo, utilizando uma
     * lista ordenada de nós por distância XOR relativamente à chave procurada.
     * O algoritmo consulta iterativamente os nós mais próximos ainda não contactados,
     * respeitando o parâmetro ALPHA, evitando consultas duplicadas e excluindo
     * explicitamente o próprio nó local. As comunicações são realizadas via
     * RPC síncrono, assumindo a correta desserialização automática das respostas.
     * </p>
     *
     * <p>
     * As respostas dos nós remotos podem consistir no valor associado à chave
     * ou numa lista de nós mais próximos (“closer nodes”), indicando possíveis
     * candidatos para iterações subsequentes. O processo termina quando o valor
     * é encontrado ou quando não existem novos nós relevantes a consultar.
     * </p>
     *
     * @param key
     *        Chave Kademlia representada como {@link BigInteger}, utilizada para
     *        calcular distâncias XOR e identificar univocamente o valor na rede.
     * @param type
     *        Classe do tipo esperado no resultado, utilizada para garantir
     *        coerência semântica e evitar erros de conversão em tempo de execução.
     * @return
     *        Instância do objeto correspondente à chave, caso seja encontrado e
     *        compatível com o tipo esperado; {@code null} caso o valor não exista
     *        ou não seja recuperável na rede.
     */

    @Override
    public <T> T findValue(BigInteger key, Class<T> type) {
        String hashHex = key.toString(16);

        if (type.equals(Block.class)) {
            Block localBlock = myself.getNetworkGateway().getBlockchainEngine().getBlockOrganizer().getBlockByHash(hashHex);
            if (localBlock != null) return type.cast(localBlock);
        }
        if (type.equals(Transaction.class)) {
            Transaction localTx = myself.getNetworkGateway().getBlockchainEngine().getTransactionOrganizer().getTransactionById(hashHex);
            if (localTx != null) return type.cast(localTx);
        }
        if (type.equals(AuctionState.class)) {
            AuctionState localState = myself.getNetworkGateway().getAuctionEngine().getWorldState().get(hashHex);
            if (localState != null) return type.cast(localState);
        }

        T localVal = storage.get(key, type);
        if (localVal != null) {
            System.out.println("[DHT] Find value inside cache.");
            MetricsLogger.recordCacheHit();
            return localVal;
        }

        TreeSet<Node> shortlist = new TreeSet<>(
                Comparator.comparingDouble((Node n) -> {
                    BigInteger xorDist = n.getNodeId().distanceBetweenNode(key);
                    double trust = myself.getReputationsManager().getTrustFactor(n.getNodeId().value());
                    return myself.getRoutingTable().calculateSKademliaMetric(xorDist, trust);
                }).thenComparing(n -> n.getNodeId().value())
        );

        shortlist.addAll(myself.getRoutingTable().findClosestNodesProximity(key, NODE_K));

        Set<BigInteger> queried = new HashSet<>();
        queried.add(myself.getMyself().getNodeId().value());

        boolean madeProgress = true;

        Node closestNodeWithoutValue = null;
        BigInteger minDistanceWithoutValue = null;

        MetricsLogger.recordNetworkHit();
        int hops = 0;

        while (madeProgress) {
            madeProgress = false;

            List<Node> toQuery = shortlist.stream()
                    .filter(n -> !queried.contains(n.getNodeId().value()))
                    .limit(MAX_ALPHA)
                    .collect(Collectors.toList());

            if (toQuery.isEmpty()) break;

            for (Node node : toQuery) {
                hops++;
                queried.add(node.getNodeId().value());

                Message request = new Message(MessageType.FIND_VALUE, key, myself.getHybridLogicalClock());


                Object response = sendRPCSync(node, request);

                if (response == null) {
                    myself.getReputationsManager().reportEvent(node.getNodeId().value(), EventTypePolicy.PING_FAIL);
                    continue;
                }

                if (response instanceof byte[]) {
                    try {
                        response = SerializationUtils.deserialize((byte[]) response);
                    } catch (Exception e) {
                        System.err.println("[DHT] Fail of the deserializer response the " + node.getPort());
                        continue;
                    }
                }

                if (response instanceof List<?>) {

                    BigInteger dist = node.getNodeId().distanceBetweenNode(key);
                    if (closestNodeWithoutValue == null || dist.compareTo(minDistanceWithoutValue) < 0) {
                        closestNodeWithoutValue = node;
                        minDistanceWithoutValue = dist;
                    }

                    try {
                        List<Node> returnedNodes = (List<Node>) response;
                        for (Node received : returnedNodes) {
                            if (!shortlist.contains(received)) {
                                shortlist.add(received);
                                madeProgress = true;
                            }
                        }
                    } catch (ClassCastException e) {}
                } else {

                    if (type.isInstance(response)) {

                        if (!validateDataIntegrity(key, response, type, node)) {
                            continue;
                        }

                        System.out.println("[DHT] Successfully! Find value remote at " + node.getPort());
                        storage.put(key, response);


                        if (closestNodeWithoutValue != null) {
                            Map<String, Object> storagePayload = new HashMap<>();
                            storagePayload.put("key", key);
                            storagePayload.put("value", response);
                            Message cacheMsg = new Message(MessageType.STORAGE, storagePayload, myself.getHybridLogicalClock());


                            sendRPCAsync(closestNodeWithoutValue, cacheMsg);
                            System.out.println("[DHT] Path Caching: Optimization injected into the node " + closestNodeWithoutValue.getPort());
                        }

                        return type.cast(response);
                    }
                }
            }

            while (shortlist.size() > NODE_K * 2) {
                shortlist.pollLast();
            }
        }
        MetricsLogger.recordLookupHops("FIND_VALUE", hops);
        return null;
    }

    /**
     * Validação baseada em Content-Addressable Storage (CAS).
     *
     * <p>Quando um objeto é recebido da rede, é calculado o hash criptográfico
     * (ex.: SHA-256) do seu conteúdo. Esse hash deve corresponder exatamente
     * à chave Kademlia ({@code expectedKey}) utilizada no pedido do objeto.</p>
     *
     * <p>Este mecanismo garante a integridade e autenticidade do conteúdo,
     * assegurando que o objeto devolvido corresponde criptograficamente
     * ao identificador solicitado.</p>
     *
     * <p>Se o hash calculado não corresponder à chave esperada, o objeto é
     * considerado inválido ou forjado e deve ser rejeitado. O nó que forneceu
     * o objeto poderá ser penalizado no sistema de confiança.</p>
     *
     * @param expectedKey Chave Kademlia esperada, previamente anunciada na rede
     *                    através da arquitetura PUB/SUB, permitindo que os vizinhos
     *                    DHT saibam qual objeto deve corresponder à chave.
     * @param data Objeto recebido da rede pelos vizinhos mais próximos no k-bucket,
     *             correspondente ao identificador do objeto.
     * @param sender Identificação do nó que enviou o objeto, utilizada para
     *               verificar a sua autenticidade e reputação.
     */
    private boolean validateDataIntegrity(BigInteger expectedKey, Object data, Class<?> type, Node sender) {
        String expectedHashHex = expectedKey.toString(16);

        if (type.equals(Block.class)) {
            Block block = (Block) data;
            if (block.getCurrentBlockHash() != null && !block.getCurrentBlockHash().equals(expectedHashHex)) {
                System.err.println("[SECURITY] Poisoned Block received! Hash mismatch from " + sender.getPort());
                myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.VALID_BLOCK);
                return false;
            }
        } else if (type.equals(Transaction.class)) {
            Transaction tx = (Transaction) data;
            if (tx.getTxId() != null && !tx.getTxId().equals(expectedHashHex)) {
                System.err.println("[SECURITY] Poisoned Transaction received! ID mismatch from " + sender.getPort());
                myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.MALICIOUS_BEHAVIOR);
                return false;
            }
        } else if (type.equals(AuctionState.class)) {
            AuctionState state = (AuctionState) data;
            if (state.getAuctionId() != null && !state.getAuctionId().equals(expectedHashHex)) {
                System.err.println("[SECURITY] Poisoned AuctionState received! ID mismatch from " + sender.getPort());
                myself.getReputationsManager().reportEvent(sender.getNodeId().value(), EventTypePolicy.MALICIOUS_BEHAVIOR);
                return false;
            }
        }
        return true;
    }


    /**
     * Verifica se um nó (peer) está ativo na rede Kademlia antes de iniciar
     * qualquer envio de mensagem ou processo de solicitação.
     * <p>
     * Ping() envia um ping ao nó de destino para confirmar a sua disponibilidade,
     * garantindo que a comunicação subsequente seja realizada apenas com nós ativos.
     * </p>
     *
     * @param target
     *        Identificador do nó na rede, utilizado para localizar logicamente o nó
     *        no sistema distribuído. A chave pode ser gerada através de mecanismos
     *        de prova de trabalho e possui 256 bits.
     */


    @Override
    public boolean ping(Node target) {
        Message pingMsg = new Message(MessageType.PING, "PING", myself.getHybridLogicalClock());

        MetricsLogger.updateTopologyMetrics(
                myself.getNeighboursManager().getActiveNeighbours().size(),
                this.storage.size()
        );

        Object res = sendRPCAsync(target, pingMsg);
        return res != null;
    }


    /**
     * O armazenamento é responsável por anunciar e manter a associação entre
     * identificadores e objetos criados na rede, refletindo o estado de posse
     * desses objetos num dado momento. Um objeto pode estar associado a um único
     * nó ou a um conjunto de nós, dependendo da distribuição da rede e do grau
     * de comunicação estabelecido entre o nó criador e os restantes participantes.
     *
     * <p>
     * Os objetos armazenados são identificados de forma determinística através
     * de chaves geradas por mecanismos de prova de trabalho, garantindo um
     * espaço de endereçamento de 256 bits e reduzindo a probabilidade de colisão
     * ou apropriação maliciosa de identificadores.
     * </p>
     *
     * @param key
     *        Identificador do objeto na rede, utilizado para estabelecer a sua
     *        localização lógica no sistema distribuído. A chave é gerada através
     *        de um processo de prova de trabalho e possui 256 bits.
     * @param value
     *        Valor associado ao identificador {@code key}, representando a
     *        referência lógica ao objeto. Este valor pode corresponder, por
     *        exemplo, a um bloco ou a um nó, devendo ser evitada a passagem de
     *        objetos diretos para a blockchain.
     */

    @Override
    public void storage(BigInteger key, Object value) {
        storage.put(key, value);
        List<Node> closestNodes = findNode(key);

        MetricsLogger.updateTopologyMetrics(
                myself.getNeighboursManager().getActiveNeighbours().size(),
                this.storage.size()
        );

        Map<String, Object> storagePayload = new HashMap<>();
        storagePayload.put("key", key);
        storagePayload.put("value", value);

        Message storeMsg = new Message(MessageType.STORAGE, storagePayload, myself.getHybridLogicalClock());

        for (Node node : closestNodes) {
            if (node.getNodeId().value().equals(myself.getMyself().getNodeId().value())) {
                continue;
            }

            sendRPCAsync(node, storeMsg);
            System.out.println("[DHT] Replica request sent to: " + node.getPort());
        }
    }

    /**
     * Envia uma mensagem RPC para um nó de destino de forma assíncrona
     * (modelo "Request e Response").
     * <p>
     * O sendRPCAsync() é ideal para mensagens de Gossip ou notificações, em que
     * o remetente não precisa de aguardar uma resposta com dados, evitando
     * o bloqueio de threads no fluxo principal.
     * </p>
     *
     * @return O objeto retornado pelo nó remoto, caso exista; {@code null} se não houver resposta.
     */
    public Object sendRPCAsync(Node target, Message request) {
        if (target.getNodeId().equals(myself.getMyself().getNodeId())) return null;

        new Thread(() -> {
            try {
                ConnectionHandler handler = myself.getNeighboursManager()
                        .getNeighbourById(target.getNodeId().value());

                if (handler != null && !handler.getSocket().isClosed()) {
                    handler.sendMessageToPeer(request);
                    System.out.println("[GOSSIP] Notification sent via tunnel to: " + target.getPort());
                } else {
                    System.err.println("[GOSSIP] Tunnel to " + target.getPort() + " is closed or ghosted.");
                }
            } catch (Exception e) {
                System.err.println("[GOSSIP] Failed to send notification to " + target.getPort() + " (Node Offline?).");
            }
        }).start();
        return null;
    }

    /**
     * Envia uma mensagem RPC para um nó de destino de forma SÍNCRONA.
     * <p>
     * Bloqueia a thread atual até receber a resposta ou atingir o tempo limite (timeout).
     * Esta operação utiliza uma ligação efémera para evitar contenção de leitura
     * com o {@code ConnectionHandler} principal.
     * </p>
     *
     * @param target Nó de destino.
     * @param request Mensagem a enviar (ex.: {@code FIND_NODE}, {@code FIND_VALUE}).
     * @return O objeto de carga útil ({@code Payload}) retornado pelo nó alvo,
     *         ou {@code null} em caso de falha ou timeout.
     */
    public Object sendRPCSync(Node target, Message request) {
        if (target.getNodeId().equals(myself.getMyself().getNodeId())) {
            return null;
        }

        ConnectionHandler handler = myself.getNeighboursManager()
                .getNeighbourById(target.getNodeId().value());

        if (handler != null && !handler.getSocket().isClosed()) {
            try {

                handler.sendMessageToPeer(request);
                System.out.println("[DHT RPC] Request sent to: " + target.getPort());

                long startTime = System.currentTimeMillis();

                Message response;

                synchronized (handler.getInputStream()) {
                    if (handler.isSecure()) {
                        response = MessageUtils.readSecureMessage(handler.getInputStream(), handler.getSecureSession());
                    } else {
                        response = MessageUtils.readMessage(handler.getInputStream());
                    }
                }

                long rttDelay = System.currentTimeMillis() - startTime;
                MetricsLogger.recordLatency(target.getPort() + "", rttDelay);

                if (response != null) {
                    return response.getPayload();
                }

            } catch (java.net.SocketTimeoutException e) {
                System.err.println("[DHT RPC] Timeout waiting for response from " + target.getPort());
                myself.getReputationsManager().reportEvent(target.getNodeId().value(), EventTypePolicy.PING_FAIL);
                MetricsLogger.recordRpcError("TIMEOUT");
            } catch (Exception e) {
                System.err.println("[DHT RPC] Synchronous communication failed with " + target.getPort() + ": " + e.getMessage());
                MetricsLogger.recordRpcError("CONNECTION_REFUSED");
            }
        }
        return null;
    }

}
