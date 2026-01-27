package org.graph.infrastructure.network.kademlia;

import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.p2p.Peer;
import org.graph.infrastructure.provider.KademliaIController;
import org.graph.infrastructure.storage.StorageDHT;
import org.graph.infrastructure.utils.Base64Utils;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

import static org.graph.infrastructure.utils.Constants.MAX_ALPHA;
import static org.graph.infrastructure.utils.Constants.NODE_K;

public class KademliaNetwork implements KademliaIController {
    private Peer myself;
    private StorageDHT storage;
    private static final int TIMEOUT_MS = 3000;

    public KademliaNetwork(Peer myself) {
        this.storage = new StorageDHT();
        this.myself = myself;
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
                    .collect(Collectors.toList());

            if (toQuery.isEmpty()) break;

            for (Node node : toQuery) {
                queried.add(node.getNodeId().value());

                Message findMsg = new Message(MessageType.FIND_NODE, targetId);
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
                Message request = new Message(MessageType.FIND_VALUE, key);
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
        Message pingMsg = new Message(MessageType.PING, "PING");
        Object res = sendRPC(target, pingMsg);
        return res != null;
    }


    @Override
    public void storage(BigInteger key, Object value) {
        // 1. Encontrar os K nós mais próximos na rede (Lookup)
        List<Node> closestNodes = findNode(key);

        // 2. Criar mensagem de Storage
        // Encapsulamos chave e valor num HashMap ou Objeto próprio para envio
        Map<String, Object> storagePayload = new HashMap<>();
        storagePayload.put("key", key);
        storagePayload.put("value", value);

        // A mensagem transporta o mapa serializável
        Message storeMsg = new Message(MessageType.STORAGE, storagePayload);

        for (Node node : closestNodes) {
            if (node.equals(myself.getMyself())) {
                // Armazenamento Local (Thread-Safe via StorageDHT)
                storage.put(key, value);
                System.out.println("[DHT] Guardado localmente: " + key);
            } else {
                // Envio Remoto (Thread separada para não bloquear)
                new Thread(() -> {
                    sendRPC(node, storeMsg);
                    System.out.println("[DHT] Replicado para: " + node.getHost());
                }).start();
            }
        }
    }

    private Object sendRPC(Node target, Message request) {
        // Não enviar para si mesmo via rede
        if (target.getNodeId().equals(myself.getMyself().getNodeId())) {
            return null;
        }

        // Try-with-resources garante que o Socket FECHA no final do bloco
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.getHost(), target.getPort()), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                // 1. Preparar Payload: Object -> Bytes -> Base64
                // Nota: Assumimos que request.getPayload() já devolve bytes serializados ou string
                // Se Message suportar Object, serializamos aqui:
                byte[] rawBytes = SerializationUtils.serialize(request); // Serializa a mensagem toda
                String base64Message = Base64Utils.encode(rawBytes);     // Converte para Base64

                // 2. Enviar Protocolo: [Tamanho (Int)] + [Base64 String (UTF-8)]
                byte[] dataToSend = base64Message.getBytes("UTF-8");
                out.writeInt(dataToSend.length);
                out.write(dataToSend);
                out.flush();

                // 3. Ler Resposta
                int length = in.readInt();
                byte[] receivedBytes = new byte[length];
                in.readFully(receivedBytes);

                // 4. Descodificar: UTF-8 -> Base64 String -> Bytes -> Objeto
                String receivedBase64 = new String(receivedBytes, "UTF-8");
                byte[] objectBytes = Base64Utils.decodeToBytes(receivedBase64);

                return SerializationUtils.deserialize(objectBytes);
            }
        } catch (Exception e) {
            // Log de erro: Nó indisponível
            return null;
        }
    }


}
