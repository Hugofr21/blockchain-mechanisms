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
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
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

    private SecureSession secureSession = null;
    private boolean isSecure = false;

    public ConnectionHandler(Socket socket, Peer myPeer, Logger mLogger) {
        this.socket = socket;
        this.myPeer = myPeer;
        this.logger = mLogger;
        this.running = true;
    }

    public Peer getPeer() { return myPeer;}
    public Node getRemoteNode() {return remoteNode;}
    public void setRemoteNode(Node bootstrapNode) {
        remoteNode = bootstrapNode;
    }
    public Socket getSocket() {
        return socket;
    }

    public void enableSecureTransport(byte[] ecdhSharedSecret) throws Exception {
        this.secureSession = new SecureSession(ecdhSharedSecret);
        this.isSecure = true;
    }

    public SecureSession getSecureSession() {
        return secureSession;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public DataOutputStream getOutputStream() {
        if (outputStream == null) {
            try {
                initStreams();
            }catch (Exception message){
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

                    if (myPeer.getGlobalScheduler() != null) {
                        myPeer.getGlobalScheduler().submit(message, this);
                    } else {
                        dispatch(message);
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
            case PONG -> handlePong(message.getPayload());
            case FIND_NODE -> handleFindNode(message.getPayload());
            case FIND_VALUE -> handleFindValue(message.getPayload());
            case STORAGE -> handleStorage(message.getPayload());
            case RESPONSE_NODES -> handleResponseNode(message.getPayload());
            case ACK -> handleAck(message.getPayload());
            case GET_STATUS -> new GetStatusStrategy().handle(message, this);
            case CHAIN_STATUS_RESPONSE -> new ChainStatusResponseStrategy().handle(message, this);
            case GET_BLOCK -> new GetBlockStrategy().handle(message, this);
            case BLOCK -> new BlockStrategy().handle(message, this);
            case INV_DATA -> new InvStrategy().handle(message, this);
            case TRANSACTION -> handleTransaction(message.getPayload());
            case GET_BLOCKS_BATCH -> new GetBlocksBatchStrategy().handle(message, this);
            case BLOCK_BATCH -> new BlockBatchStrategy().handle(message, this);
            default -> logger.warning("Unhandled message type: " + message.getType());
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
                        System.out.println("Amount: " + bid.bidPrice() + " €");
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

    private void handleAck(Object payload) {

    }

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
                logger.warning("[DHT] Invalid format in handleStorage. Expected: Map, received: [Insert value here].: " +
                        (payload != null ? payload.getClass().getSimpleName() : "null"));
            }

        } catch (Exception e) {
            logger.severe("Critical error in handleStorage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFindValue(Object payload) {
        try {

            if (payload instanceof byte[]) {
                payload = SerializationUtils.deserialize((byte[]) payload);
            }


        } catch (Exception e) {
            logger.severe("Critical error in handleStorage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFindNode(Object rawPayload) {
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
            Message response = new Message(MessageType.RESPONSE_NODES, container, myPeer.getHybridLogicalClock());
            sendMessageToPeer(response);
            getOutputStream().flush();

            logger.severe("[FIND_NODE] Respondi com " + rawList.size() + " vizinhos.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia uma mensagem para o nó remoto.
     * Decide automaticamente se usa o túnel encriptado (AES-GCM) ou o túnel aberto,
     * dependendo do estado atual da sessão ECDH.
     */
    public void sendMessageToPeer(Message message) {
        try {
            if (this.isSecure) {
                MessageUtils.sendSecureMessage(getOutputStream(), message, this.secureSession);
            } else {
                MessageUtils.sendMessage(getOutputStream(), message);
            }
        } catch (Exception e) {
            logger.severe("[SECURITY] Falha crítica ao enviar mensagem: " + e.getMessage());
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
        myPeer.getNeighboursManager().updateTimestamp(remoteNode.getNodeId().value());
        myPeer.getRoutingTable().touchNode(remoteNode.getNodeId().value());
    }

    private void handlePing(Message pongMessage) {

        myPeer.getNeighboursManager().updateTimestamp(remoteNode.getNodeId().value());
        myPeer.getRoutingTable().touchNode(remoteNode.getNodeId().value());
        Message pongMsg = new Message(MessageType.PONG, "PONG", myPeer.getHybridLogicalClock());

        long endNano = System.nanoTime();
        Object payload = pongMessage.getPayload();

        if (payload instanceof Long startNano) {
            long durationNano = endNano - startNano;
            double rttMs = durationNano / 1_000_000.0;
            MetricsLogger.recordLatency(remoteNode.getNodeId().value(), rttMs);
        }

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
