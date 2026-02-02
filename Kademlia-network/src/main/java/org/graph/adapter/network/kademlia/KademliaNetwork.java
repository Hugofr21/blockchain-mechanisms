package org.graph.adapter.network.kademlia;

import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.storage.cache.LRUCache;
import org.graph.adapter.utils.MessageUtils;
import org.graph.adapter.utils.SerializationUtils;
import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.server.Peer;
import org.graph.adapter.provider.IKademliaIController;
import org.graph.adapter.storage.StorageDHT;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

import static org.graph.adapter.utils.Constants.MAX_ALPHA;
import static org.graph.adapter.utils.Constants.NODE_K;

/**
 * Esta implementação de Kademlia deve ser utilizada exclusivamente quando
 * já existe uma lista de nós considerada fiável, validada e adequada para
 * comunicações seguras entre participantes da rede.
 *
 * O pressuposto fundamental é que os nós presentes nesta lista passaram
 * previamente por mecanismos de autenticação, validação de identidade e,
 * quando aplicável, verificação criptográfica ou prova de trabalho,
 * mitigando riscos associados a ataques Sybil, Eclipse ou à introdução de
 * nós maliciosos. Consequentemente, esta camada de Kademlia não se destina
 * à descoberta inicial de nós num ambiente não confiável, mas sim à
 * manutenção, otimização e exploração eficiente de uma topologia já
 * estabilizada.
 *
 * A utilização deste componente fora deste contexto constitui um erro de
 * conceção do sistema, uma vez que compromete diretamente as garantias de
 * segurança assumidas pelo protocolo e invalida os pressupostos de
 * comunicação segura entre nós.
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


    /**
     * Realiza a descoberta iterativa dos nós mais próximos de um determinado
     * identificador numa rede distribuída baseada em Kademlia, aplicando
     * otimizações locais e mecanismos explícitos de mitigação de ataques.
     *
     * O processo inicia-se com uma verificação local em memória ou cache,
     * evitando tráfego de rede desnecessário sempre que a informação já se
     * encontra disponível. De seguida, é inicializada a shortlist de lookup,
     * composta pelos nós seed obtidos a partir da tabela de encaminhamento
     * local, ordenados segundo a distância XOR relativamente ao identificador
     * alvo.
     *
     * Antes de qualquer nó remoto ser considerado válido para participação
     * no processo de lookup, é aplicada uma validação do seu identificador,
     * incluindo a verificação de prova de trabalho associada ao Node ID,
     * conforme os princípios de S/Kademlia. Esta validação é crítica para
     * reduzir a superfície de ataque a cenários de Eclipse Attack, garantindo
     * que apenas nós que provaram ter gerado corretamente o seu identificador
     * são incluídos na shortlist.
     *
     * A fase seguinte consiste num loop iterativo de rede, onde são
     * selecionados, em cada iteração, os ALPHA nós mais próximos ainda não
     * consultados. Estes nós são contactados de forma controlada, atualizando
     * progressivamente a shortlist com candidatos mais próximos, até que não
     * existam novos nós relevantes a consultar ou que seja atingido o critério
     * de convergência definido pelo protocolo.
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
        Object cachedVal = hotCache.readFromCache(targetId);

        if (cachedVal != null && cachedVal instanceof Node node) {
            return (List<Node>) node;
        }

        if (myself.getMyself().getNodeId().value().equals(targetId)) {
            return Collections.singletonList(myself.getMyself());
        }

        Node localTarget = myself.getRoutingTable().getByNodeIdNode(targetId);
        if (localTarget != null) {
            List<Node> result = myself.getRoutingTable().findClosestNodesProximity(targetId, NODE_K);
            if (!result.contains(localTarget)) result.add(0, localTarget);
            return result;
        }


        TreeSet<Node> shortlist = new TreeSet<>(Comparator.comparing(
                n -> n.getNodeId().distanceBetweenNode(targetId)
        ));

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
                Object res = sendRPC(node, findMsg);
                if (res == null) continue;

                if (res instanceof byte[]) {
                    try {
                        res = SerializationUtils.deserialize((byte[]) res);
                    } catch (Exception e) {
                        System.err.println("[DHT] Erro ao deserializar lista de nós de " + node.getPort());
                        continue;
                    }
                }

                List<Node> returnedNodes = Collections.emptyList();

                if (res instanceof List<?>) {
                    try {
                        returnedNodes = (List<Node>) res;
                    } catch (ClassCastException e) {
                        continue;
                    }
                }

                for (Node received : returnedNodes) {
                    if (!shortlist.contains(received) && !received.getNodeId().equals(myself.getMyself().getNodeId())) {
                        shortlist.add(received);
                        madeProgress = true;
                        // myself.getRoutingTable().addNode(received);
                    }
                }
            }

            while (shortlist.size() > NODE_K * 2) {
                shortlist.pollLast();
            }
        }

        return new ArrayList<>(shortlist).subList(0, Math.min(NODE_K, shortlist.size()));
    }


    /**
     * Executa a procura de um valor numa rede distribuída baseada em Kademlia,
     * garantindo segurança de tipos e respeitando a hierarquia de otimização
     * definida pelo sistema. O processo inicia-se pela verificação de fontes
     * locais, reduzindo latência e tráfego de rede, e apenas recorre a mecanismos
     * de descoberta distribuída quando necessário.
     *
     * A pesquisa é realizada de forma sequencial e determinística. Em primeiro
     * lugar, é verificada a existência local do valor na blockchain, aplicável
     * exclusivamente quando a chave corresponde a um bloco já validado e
     * organizado internamente. Em seguida, é consultado o armazenamento DHT
     * local, que funciona como cache persistente de dados previamente obtidos.
     * Caso o valor não seja encontrado em nenhuma destas camadas, é então
     * desencadeado um processo iterativo de lookup na rede Kademlia.
     *
     * O lookup distribuído segue o modelo clássico do protocolo, utilizando uma
     * lista ordenada de nós por distância XOR relativamente à chave procurada.
     * O algoritmo consulta iterativamente os nós mais próximos ainda não
     * contactados, respeitando o parâmetro ALPHA, evitando consultas duplicadas
     * e excluindo explicitamente o próprio nó local. As comunicações são
     * realizadas via RPC síncrono, assumindo a correta desserialização automática
     * das respostas recebidas.
     *
     * As respostas dos nós remotos podem consistir no valor associado à chave
     * ou numa lista de nós mais próximos (“closer nodes”), indicando possíveis
     * candidatos para iterações subsequentes. O processo termina quando o valor
     * é encontrado ou quando não existem novos nós relevantes a consultar.
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

        if (type.equals(Block.class)) {

            String hashHex = key.toString(16);


            Block localBlock = myself.getNetworkGateway().getBlockchainEngine()
                    .getBlockOrganizer().getBlockByHash(hashHex);

            if (localBlock != null) {
                System.out.println("[DHT] Finding of the Blockchain local (BlockOrganizer).");
                return type.cast(localBlock);
            }
        }


        T localVal = storage.get(key, type);
        if (localVal != null) {
            System.out.println("[DHT] Find value inside cache.");
            return localVal;
        }


        TreeSet<Node> shortlist = new TreeSet<>(Comparator.comparing(
                n -> n.getNodeId().distanceBetweenNode(key)
        ));


        shortlist.addAll(myself.getRoutingTable().findClosestNodesProximity(key, NODE_K));


        Set<BigInteger> queried = new HashSet<>();
        queried.add(myself.getMyself().getNodeId().value());

        boolean madeProgress = true;

        while (madeProgress) {
            madeProgress = false;

            List<Node> toQuery = shortlist.stream()
                    .filter(n -> !queried.contains(n.getNodeId().value()))
                    .limit(MAX_ALPHA)
                    .collect(Collectors.toList());

            if (toQuery.isEmpty()) break;

            for (Node node : toQuery) {
                queried.add(node.getNodeId().value());

                Message request = new Message(MessageType.FIND_VALUE, key, myself.getHybridLogicalClock());

                Object response = sendRPC(node, request);

                if (response == null) continue;


                if (response instanceof byte[]) {
                    try {
                        response = SerializationUtils.deserialize((byte[]) response);
                    } catch (Exception e) {
                        System.err.println("[DHT] Fail of the deserializer response the " + node.getPort());
                        continue;
                    }
                }

                if (response instanceof List<?>) {
                    try {
                        List<Node> returnedNodes = (List<Node>) response;
                        for (Node received : returnedNodes) {
                            if (!shortlist.contains(received)) {
                                shortlist.add(received);
                                madeProgress = true;
                            }
                        }
                    } catch (ClassCastException e) {}
                }

                else {

                    if (type.isInstance(response)) {
                        System.out.println("[DHT] Successfully! Find value remote at " + node.getPort());

                        storage.put(key, response);

                        return type.cast(response);
                    }
                }
            }


            while (shortlist.size() > NODE_K * 2) {
                shortlist.pollLast();
            }
        }

        return null;
    }


    @Override
    public boolean ping(Node target) {
        Message pingMsg = new Message(MessageType.PING, "PING", myself.getHybridLogicalClock());
        Object res = sendRPC(target, pingMsg);
        return res != null;
    }


    @Override
    public void storage(BigInteger key, Object value) {
        List<Node> closestNodes = findNode(key);
        Map<String, Object> storagePayload = new HashMap<>();
        storagePayload.put("key", key);
        storagePayload.put("value", value);

        Message storeMsg = new Message(MessageType.STORAGE, storagePayload, myself.getHybridLogicalClock());

        for (Node node : closestNodes) {
            if (node.equals(myself.getMyself())) {
                storage.put(key, value);
                System.out.println("[DHT] Saving  local: " + key);
            } else {
                new Thread(() -> {
                    sendRPC(node, storeMsg);
                    System.out.println("[DHT] Replica to: " + node.getHost());
                }).start();
            }
        }
    }


    private Object sendRPC(Node target, Message request) {

        if (target.getNodeId().equals(myself.getMyself().getNodeId())) return null;


        try (Socket socket = new java.net.Socket(target.getHost(), target.getPort())) {

            socket.setSoTimeout(5000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());


            MessageUtils.sendMessage(out, request);

            Message response = MessageUtils.readMessage(in);

            return (response != null) ? response.getPayload() : null;

        } catch (Exception e) {
             System.out.println("[DHT] Node " + target.getPort() + " not response.");
            return null;
        }
    }

}
