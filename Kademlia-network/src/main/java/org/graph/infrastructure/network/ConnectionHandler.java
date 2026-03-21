package org.graph.infrastructure.network;

import org.graph.adapter.inbound.network.Handshake;
import org.graph.adapter.outbound.network.message.auction.AuctionPayload;
import org.graph.adapter.outbound.network.message.node.FindNodePayload;
import org.graph.adapter.outbound.network.message.node.NodeInfoPayload;
import org.graph.adapter.outbound.network.message.node.NodeListPayload;
import org.graph.adapter.utils.Base64Utils;
import org.graph.domain.entities.auctions.Bid;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.entities.transaction.TransactionType;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.domain.valueobject.cryptography.PublicKeyPeer;
import org.graph.infrastructure.crypt.SecureSession;
import org.graph.infrastructure.utils.EncapsulationUtils;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.infrastructure.utils.SerializationUtils;
import org.graph.gateway.block.*;
import org.graph.server.Peer;
import org.graph.server.utils.MetricsLogger;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.graph.adapter.utils.Constants.*;

/**
 * As mensagens são persistidas em fila quando não podem ser processadas
 * imediatamente, garantindo durabilidade e evitando perda de eventos.
 * O processamento é assíncrono, não bloqueante para operações de leitura
 * e escrita, promovendo desacoplamento temporal entre produtores e
 * consumidores e aumentando a eficiência sob carga.
 **/

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private Peer myPeer;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Logger logger;
    private volatile boolean running;
    private Node remoteNode;
    private final ConcurrentHashMap<String, CompletableFuture<Message>> pendingRequests;

    private SecureSession secureSession = null;
    private boolean isSecure = false;

    public ConnectionHandler(Socket socket, Peer myPeer, Logger mLogger) {
        this.socket = socket;
        this.myPeer = myPeer;
        this.logger = mLogger;
        this.running = true;
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    public Peer getPeer() { return myPeer; }
    public Node getRemoteNode() { return remoteNode; }
    public void setRemoteNode(Node bootstrapNode) { this.remoteNode = bootstrapNode; }
    public Socket getSocket() { return socket; }

    public void enableSecureTransport(byte[] ecdhSharedSecret) throws Exception {
        this.secureSession = new SecureSession(ecdhSharedSecret);
        this.isSecure = true;
    }

    public SecureSession getSecureSession() { return secureSession; }

    public boolean isSecure() { return isSecure; }

    public DataOutputStream getOutputStream() {
        if (outputStream == null) {
            try {
                initStreams();
            } catch (Exception message) {
                System.out.println(message.getMessage());
            }
        }
        return outputStream;
    }

    public DataInputStream getInputStream() {
        if (inputStream == null) {
            try {
                initStreams();
            } catch (IOException e) {
                if (logger != null) {
                    logger.severe("Error initializing streams in getInputStream: " + e.getMessage());
                } else {
                    System.err.println("Error initializing streams: " + e.getMessage());
                }
            }
        }
        return inputStream;
    }

    /**
     * Delega o envio da mensagem e regista o CompletableFuture para
     * a thread principal do ConnectionHandler acordar a thread do Kademlia
     * quando a resposta exata chegar.
     */
    public CompletableFuture<Message> sendRPCAndAwait(Message request) {
        CompletableFuture<Message> future = new CompletableFuture<>();
        pendingRequests.put(request.getId(), future);

        try {
            sendMessageToPeer(request);
        } catch (Exception e) {
            pendingRequests.remove(request.getId());
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void run() {
        try {
            initStreams();
            logger.info("Connection loop started: " + socket.getRemoteSocketAddress());

            while (myPeer.getIsRunning() && !socket.isClosed() && running) {
                try {
                    Message message;

                    if (this.isSecure) {
                        message = MessageUtils.readSecureMessage(inputStream, this.secureSession);
                    } else {
                        message = MessageUtils.readMessage(inputStream);
                    }

                    MetricsLogger.recordInboundMessage(myPeer.getMyself().getNodeId().value());

                    logger.severe("Received message: " + message.toString());

                    if (message.getType() == MessageType.HELLO) {
                        logger.info("Redundant HELLO received in loop.");
                        continue;
                    }

                    if (message.getTimestamp() != null) {
                        myPeer.getHybridLogicalClock().update(message.getTimestamp());
                    }

                    if (message.isResponse() && pendingRequests.containsKey(message.getCorrelationId())) {
                        pendingRequests.remove(message.getCorrelationId()).complete(message);
                    } else {
                        if (myPeer.getGlobalScheduler() != null) {
                            myPeer.getGlobalScheduler().submit(message, this);
                        } else {
                            dispatch(message);
                        }
                    }

                } catch (EOFException e) {
                    logger.info("Peer closed connection.");
                    break;
                } catch (SocketTimeoutException e) {
                    continue; // Keep-alive / Heartbeat loop
                } catch (Exception e) {
                    logger.severe("Processing error: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            logger.severe("Connection stream error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    public void initStreams() throws IOException {
        if (inputStream != null && outputStream != null) return;
        if (outputStream == null) outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        if (inputStream == null) inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    public void dispatch(Message message) {
        switch (message.getType()) {
            case PING -> handlePing(message);
            case FIND_NODE -> handleFindNode(message);
            case FIND_VALUE -> handleFindValue(message);
            case RESPONSE_VALUE -> handleResponseValue(message);
            case PONG -> handlePong(message.getPayload());
            case STORAGE -> handleStorage(message.getPayload());
            case RESPONSE_NODES -> handleResponseNode(message.getPayload());
            case ACK -> handleAck(message.getPayload());
            case GET_STATUS -> new GetStatusStrategyI().handle(message, this);
            case CHAIN_STATUS_RESPONSE -> new ChainStatusResponseStrategyI().handle(message, this);
            case GET_BLOCK -> new GetBlockStrategyI().handle(message, this);
            case BLOCK -> new BlockStrategyI().handle(message, this);
            case INV_DATA -> new InvStrategyI().handle(message, this);
            case TRANSACTION -> handleTransaction(message.getPayload());
            case GET_BLOCKS_BATCH -> new GetBlocksBatchStrategy().handle(message, this);
            case BLOCK_BATCH -> new BlockBatchStrategy().handle(message, this);
            default -> logger.warning("Unhandled message type: " + message.getType());
        }
    }

    /**
     * REDE DE SEGURANÇA: Interceta respostas FIND_VALUE atrasadas (Timeouts)
     * ou tentativas de injeção de pacotes maliciosos na DHT.
     */
    private void handleResponseValue(Message message) {
        if (message.getCorrelationId() != null) {
            logger.warning("[DHT] RESPONSE_VALUE órfã recebida. O pacote chegou atrasado e o pedido original já expirou (Timeout).");
        } else {
            logger.warning("[SECURITY] RESPONSE_VALUE não solicitada (Sem Correlation ID) recebida de " +
                    (remoteNode != null ? remoteNode.getPort() : "Desconhecido") + ". Possível injeção descartada.");

            if (remoteNode != null) {
                myPeer.getReputationsManager().reportEvent(remoteNode.getNodeId().value(), EventTypePolicy.MALICIOUS_BEHAVIOR);
            }
        }
    }

    private void handleTransaction(Object payload) {
        try {
            if (payload instanceof byte[]) {
                payload = SerializationUtils.deserialize((byte[]) payload);
            }

            if (payload instanceof Transaction tx) {
                if (tx.getType() == TransactionType.BID) {
                    if (tx.getData() instanceof AuctionPayload p) {
                        Bid bid = p.getBidRemote();
                        System.out.println("\n======== Receive of the transaction: ========");
                        System.out.println(" [PUB/SUB] NEW LANCE ON THE P2P NETWORK! ");
                        System.out.println(" Auction: " + bid.auctionId().substring(0,8));
                        System.out.println(" Amount: " + bid.bidPrice() + " €");
                        System.out.println("=============================================\n");
                    }
                }
                myPeer.getNetworkGateway().getBlockchainEngine()
                        .getTransactionOrganizer().addTransaction(tx);
            } else {
                logger.warning("[NETWORK] Expected Transaction, received: " +
                        (payload != null ? payload.getClass().getName() : "null"));
            }
        } catch (Exception e) {
            logger.severe("[NETWORK] Err critical an process transaction receive: " + e.getMessage());
        }
    }

    private void handleAck(Object payload) {}

    private void handleStorage(Object payload) {
        System.out.println("Received storage packet: " + payload);
        try {
            if (payload instanceof byte[]) {
                payload = SerializationUtils.deserialize((byte[]) payload);
            }

            if (payload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> storageData = (Map<String, Object>) payload;

                BigInteger key = (BigInteger) storageData.get("key");
                Object value = storageData.get("value");

                if (key != null && value != null) {
                    myPeer.getMkademliaNetwork().getStorage().put(key, value);
                } else {
                    logger.warning("[DHT] Incomplete Storage Payload (null key or value).");
                }
            } else {
                logger.warning("[DHT] Invalid format in handleStorage. Expected: Map, received: " +
                        (payload != null ? payload.getClass().getSimpleName() : "null"));
            }
        } catch (Exception e) {
            logger.severe("Critical error in handleStorage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processa pedidos FIND_VALUE. Vasculha a Cache (DHT), os Blocos,
     * as Transações Pendentes e o Estado dos Leilões.
     * Se não encontrar, devolve os nós mais próximos.
     */
    private void handleFindValue(Message requestMessage) {
        try {
            Object rawPayload = requestMessage.getPayload();
            if (rawPayload instanceof byte[]) {
                rawPayload = SerializationUtils.deserialize((byte[]) rawPayload);
            }

            if (!(rawPayload instanceof BigInteger targetKey)) {
                logger.warning("[FIND_VALUE] Invalid payload type. Expected BigInteger.");
                return;
            }

            String hashHex = targetKey.toString(16);
            Object foundValue = null;

            foundValue = myPeer.getMkademliaNetwork().getStorage().get(targetKey, Object.class);

            if (foundValue == null) {
                foundValue = myPeer.getNetworkGateway().getBlockchainEngine().getBlockOrganizer().getBlockByHash(hashHex);
            }
            if (foundValue == null) {
                foundValue = myPeer.getNetworkGateway().getBlockchainEngine().getTransactionOrganizer().getTransactionById(hashHex);
            }
            if (foundValue == null) {
                foundValue = myPeer.getNetworkGateway().getAuctionEngine().getWorldState().get(hashHex);
            }

            Message response;
            if (foundValue != null) {
                response = new Message(
                        MessageType.RESPONSE_VALUE,
                        foundValue,
                        myPeer.getHybridLogicalClock(),
                        requestMessage.getId()
                );
            } else {

                List<Node> closestNodes = myPeer.getRoutingTable().findClosestNodesProximity(targetKey, NODE_K);

                List<NodeInfoPayload> rawList = closestNodes.stream().map(n -> {
                    byte[] keyBytes = null;
                    PublicKeyPeer pkPeer = myPeer.getIsKeysInfrastructure().getNeighborPublicKeyByPeerId(n.getNodeId().value());
                    if (pkPeer != null && pkPeer.getKey() != null) keyBytes = pkPeer.getKey().getEncoded();
                    if (keyBytes == null) return null;
                    return new NodeInfoPayload(new FindNodePayload(Base64Utils.encode(n.getNodeId().value().toByteArray())), n.getHost(), n.getPort(), n.getNonce(), keyBytes, n.getNETWORK_DIFFICULTY());
                }).filter(java.util.Objects::nonNull).toList();

                NodeListPayload container = new NodeListPayload(rawList);
                response = new Message(
                        MessageType.RESPONSE_NODES,
                        container,
                        myPeer.getHybridLogicalClock(),
                        requestMessage.getId()
                );
            }

            sendMessageToPeer(response);
            getOutputStream().flush();

        } catch (Exception e) {
            logger.severe("Critical error in handleFindValue: " + e.getMessage());
        }
    }

    /**
     * Processa pedidos FIND_NODE devolvendo os N vizinhos mais próximos
     * encriptados e formatados, anexando o Correlation ID na resposta.
     */
    private void handleFindNode(Message requestMessage) {
        Object rawPayload = requestMessage.getPayload();
        try {
            BigInteger remoteId = EncapsulationUtils.decapsulationNodeId(rawPayload);

            if (remoteId == null) {
                System.err.println("[FIND_NODE] ERRO: ID desencapsulado é null. Pedido rejeitado.");
                return;
            }

            List<Node> closestNodes = myPeer.getRoutingTable().findClosestNodesProximity(remoteId, NODE_K);

            if (closestNodes.isEmpty()) {
                System.out.println("[FIND_NODE] WARNING: Routing table did not find nearest neighbors.");
            }

            List<NodeInfoPayload> rawList = closestNodes.stream()
                    .map(n -> {
                        byte[] keyBytes = null;
                        PublicKeyPeer pkPeer = myPeer.getIsKeysInfrastructure()
                                .getNeighborPublicKeyByPeerId(n.getNodeId().value());

                        if (pkPeer != null && pkPeer.getKey() != null) {
                            keyBytes = pkPeer.getKey().getEncoded();
                        }

                        if (keyBytes == null) {
                            System.out.println("[DHT] CRITICAL ERROR: Neighbor " + n.getPort() + " exists in the table but without a Public Key. Ignored.");
                            return null;
                        }

                        String idBase64 = Base64Utils.encode(n.getNodeId().value().toByteArray());
                        FindNodePayload idPayload = new FindNodePayload(idBase64);

                        return new NodeInfoPayload(
                                idPayload,
                                n.getHost(),
                                n.getPort(),
                                n.getNonce(),
                                keyBytes,
                                n.getNETWORK_DIFFICULTY()
                        );
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();

            NodeListPayload container = new NodeListPayload(rawList);

            Message response = new Message(
                    MessageType.RESPONSE_NODES,
                    container,
                    myPeer.getHybridLogicalClock(),
                    requestMessage.getId()
            );

            sendMessageToPeer(response);
            getOutputStream().flush();

            logger.severe("[FIND_NODE] Response com " + rawList.size() + " neighbour.");

        } catch (Exception e) {
            System.err.println("[ERROR] Receive find node " + e.getMessage());
        }
    }

    public void sendMessageToPeer(Message message) {
        try {
            if (this.isSecure) {
                MessageUtils.sendSecureMessage(getOutputStream(), message, this.secureSession);
            } else {
                MessageUtils.sendMessage(getOutputStream(), message);
            }
        } catch (Exception e) {
            logger.severe("[SECURITY] Failed critical an send message: " + e.getMessage());
            this.closeConnection();
        }
    }

    private void handleResponseNode(Object rawPayload) {
        System.out.println("[DISCOVERY] I received a response from us.");

        NodeListPayload container = EncapsulationUtils.decapsulationListNodes(rawPayload);

        if (container != null) {
            List<NodeInfoPayload> list = container.nodes();

            if (!list.isEmpty() && this.remoteNode != null) {
                myPeer.getReputationsManager().reportEvent(
                        this.remoteNode.getNodeId().value(),
                        EventTypePolicy.FIND_NODE_USEFUL
                );
            }

            for (NodeInfoPayload info : list) {
                processSingleNodeInfo(info);
            }
        } else {
            System.err.println("[ERROR] Invalid payload. Expected NodeListPayload, received: "
                    + (rawPayload == null ? "null" : rawPayload.getClass().getName()));
        }
    }

    private void processSingleNodeInfo(NodeInfoPayload info) {
        try {
            BigInteger remoteId = EncapsulationUtils.decapsulationNodeId(info.nodeId());
            System.out.println("[DEBUG] Valid candidate ID: " + remoteId);

            if (remoteId.equals(myPeer.getMyself().getNodeId().value())) return;

            boolean isKnown = myPeer.getRoutingTable().getByNodeIdNode(remoteId) != null;
            boolean isConnected = myPeer.getNeighboursManager().isNodeConnected(remoteId);

            if (!isKnown && !isConnected) {
                System.out.println("[DISCOVERY] Starting verification for: " + info.host() + ":" + info.port());
                Handshake.connectAndVerify(info.host(), info.port(), myPeer);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Logical error processing node: " + e.getMessage());
        }
    }

    private void handlePong(Object payload) {
        System.out.println("[PONG] Received a pong: " + payload);
        if (remoteNode != null) {
            myPeer.getNeighboursManager().updateTimestamp(remoteNode.getNodeId().value());
            myPeer.getRoutingTable().touchNode(remoteNode.getNodeId().value());
        }
    }

    private void handlePing(Message requestMessage) {
        if (remoteNode != null) {
            myPeer.getNeighboursManager().updateTimestamp(remoteNode.getNodeId().value());
            myPeer.getRoutingTable().touchNode(remoteNode.getNodeId().value());
        }

        Message pongMsg = new Message(
                MessageType.PONG,
                "PONG",
                myPeer.getHybridLogicalClock(),
                requestMessage.getId()
        );

        sendMessageToPeer(pongMsg);
    }

    public void closeConnection() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warning("Error closing socket: " + e.getMessage());
            }
        }

        if (remoteNode != null) {
            myPeer.getNeighboursManager().removeConnection(remoteNode.getNodeId().value());
            myPeer.getRoutingTable().removeNode(remoteNode);
        }
    }
}