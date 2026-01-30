package org.graph.adapter.network.kademlia;

import org.graph.adapter.storage.cache.LRUCache;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.adapter.p2p.Peer;
import org.graph.adapter.provider.KademliaIController;
import org.graph.adapter.storage.StorageDHT;
import org.graph.adapter.utils.Base64Utils;
import org.graph.adapter.utils.SerializationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.graph.adapter.utils.Constants.MAX_ALPHA;
import static org.graph.adapter.utils.Constants.NODE_K;

public class KademliaNetwork implements KademliaIController {
    private final Peer myself;
    private final StorageDHT storage;
    private static final int TIMEOUT_MS = 3000;
    private final LRUCache<BigInteger, Object> hotCache;

    public KademliaNetwork(Peer myself) {
        this.storage = new StorageDHT();
        this.myself = myself;
        this.hotCache = new LRUCache<>();
    }


   /*
     // 1. Verificação Local (Otimização)
     // 2. Inicialização da Lookup List (Shortlist)
    // S/Kademlia Check: Validar PoW do ID para evitar Eclipse Attacks
    // Só adicionamos à lista se o nó provar que gerou o ID corretamente
    // && validateNodeId(received)
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

        // 2. Inicialização da Lookup List (Shortlist)
        TreeSet<Node> shortlist = new TreeSet<>(Comparator.comparing(
                n -> n.getNodeId().distanceBetweenNode(targetId)
        ));

        // Seed nodes da tabela local
        List<Node> localClosest = myself.getRoutingTable().findClosestNodesProximity(targetId, NODE_K);
        shortlist.addAll(localClosest);

        Set<BigInteger> queried = new HashSet<>();
        queried.add(myself.getMyself().getNodeId().value());

        boolean madeProgress = true;

        // 3. Loop Iterativo de Rede (Iterative Lookup)
        while (madeProgress && !shortlist.isEmpty()) {
            madeProgress = false;

            // Seleciona os ALPHA nós mais próximos ainda não consultados
            List<Node> toQuery = shortlist.stream()
                    .filter(n -> !queried.contains(n.getNodeId().value()))
                    .limit(MAX_ALPHA)
                    .toList();

            if (toQuery.isEmpty()) break;

            for (Node node : toQuery) {
                queried.add(node.getNodeId().value());

                Message findMsg = new Message(MessageType.FIND_NODE, targetId, myself.getHybridLogicalClock());
                Object res = sendRPC(node, findMsg);

                List<Node> returnedNodes = Collections.emptyList();

                if (res instanceof List<?>) {
                    try {
                        returnedNodes = (List<Node>) res;
                    } catch (ClassCastException e) {
                        continue;
                    }
                }

                for (Node received : returnedNodes) {
                    if (!shortlist.contains(received) ) {
                        shortlist.add(received);
                        madeProgress = true;
                    }
                }
            }

            while (shortlist.size() > NODE_K * 2) {
                shortlist.pollLast();
            }
        }

        return new ArrayList<>(shortlist).subList(0, Math.min(NODE_K, shortlist.size()));
    }


    //1. verifiar local tem o valor
    //2. verifica ultimas consult se tem o valor LRU
    //3. Buscar resucrisva do valor aos aphas k
    //4. Se iver esse k o valor deolve
    //5 Se nao devolvr uma lista de nod e proximosapra id
    @Override
    public Object findValue(BigInteger key) {
        Object localVal = storage.get(key, Object.class);
        if (localVal != null) return localVal;

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

                // RPC: FIND_VALUE (Payload é a chave)
                Message request = new Message(MessageType.FIND_VALUE, key, myself.getHybridLogicalClock());
                Object response = sendRPC(node, request);

                if (response == null) continue; // Timeout ou erro

                // --- LÓGICA CRÍTICA DE FIND_VALUE ---

                // CASO A: O nó retornou o VALOR (Sucesso!)
                // Assumimos que se NÃO for uma lista de nós, é o objeto procurado.
                // Deve-se melhorar esta verificação dependendo do tipo de Objeto (Block, Transaction, etc.)
                if (!(response instanceof List<?>)) {
                    // Opcional: Cachear o valor localmente antes de retornar (LRU Cache)
                    storage.put(key, response);
                    return response;
                }

                // CASO B: O nó retornou Vizinhos (não tem o valor, mas sabe quem está perto)
                // Continuamos a busca iterativa
                List<Node> returnedNodes = (List<Node>) response;

                for (Node received : returnedNodes) {
                    // S/Kademlia Check: Proteção contra Sybil/Eclipse
                    // && validateNodeId(received)
                    if (!shortlist.contains(received)) {
                        shortlist.add(received);
                        madeProgress = true;
                    }
                }
            }

            // Limpeza da shortlist para manter eficiência
            while (shortlist.size() > NODE_K * 2) {
                shortlist.pollLast();
            }
        }

        // Se o loop terminar e não encontrarmos o valor, retornamos null.
        // O Kademlia puro não retorna os nós mais próximos no findValue se falhar,
        // mas algumas implementações retornam para o cliente decidir. Aqui retornamos null (não encontrado).
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
                System.out.println("[DHT] Guardado localmente: " + key);
            } else {
                new Thread(() -> {
                    sendRPC(node, storeMsg);
                    System.out.println("[DHT] Replicado para: " + node.getHost());
                }).start();
            }
        }
    }

    private Object sendRPC(Node target, Message request) {

        if (target.getNodeId().equals(myself.getMyself().getNodeId())) {
            return null;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.getHost(), target.getPort()), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                MessageUtils.sendMessage(out, request);


                Message response = MessageUtils.readMessage(in);

                if (response == null) return null;

                return response.getPayload();
            }
        } catch (IOException | ClassNotFoundException e) {
             System.err.println("[DHT] Error sending request " + e.getMessage());
            return null;
        }
    }


}
