package org.graph.infrastructure.network;


import org.graph.adapter.inbound.network.Handshake;
import org.graph.adapter.outbound.network.message.node.FindNodePayload;
import org.graph.adapter.outbound.network.message.node.NodeInfoPayload;
import org.graph.adapter.outbound.network.message.node.NodeListPayload;
import org.graph.adapter.utils.Base64Utils;
import org.graph.infrastructure.utils.EncapsulationUtils;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.infrastructure.utils.SerializationUtils;
import org.graph.gateway.block.*;
import org.graph.server.Peer;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.graph.adapter.utils.Constants.*;
import static org.graph.adapter.utils.MessageUtils.readMessage;
import static org.graph.adapter.utils.MessageUtils.sendMessage;

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
    private BrokerEvent mBrokerEvent;

    public ConnectionHandler(Socket socket, Peer myPeer, Logger mLogger, BrokerEvent brokerEvent) {
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
                    Message message = readMessage(inputStream);
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
                        // Fallback se não quiseres usar a fila global
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
            case PING -> handlePing(message.getPayload());
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
            default -> logger.warning("Unhandled message type: " + message.getType());
        }
    }


    private void handleAck(Object payload) {

    }

    private void handleStorage(Object payload) {
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
           BigInteger remoteId =  EncapsulationUtils.encapsulationNodeId(rawPayload);
            List<Node> closestNodes = myPeer.getRoutingTable().findClosestNodesProximity(remoteId, NODE_K);
            List<NodeInfoPayload> rawList = closestNodes.stream()
                    .map(n -> new NodeInfoPayload(
                            new FindNodePayload(Base64Utils.encode(n.getNodeId().value().toByteArray())),
                            n.getHost(),
                            n.getPort()
                    ))
                    .toList();
            NodeListPayload container = new NodeListPayload(rawList);
            Message response = new Message(MessageType.RESPONSE_NODES, container, myPeer.getHybridLogicalClock());
            MessageUtils.sendMessage(getOutputStream(), response);
            getOutputStream().flush();

            System.out.println("[FIND_NODE] I sent a list with " + closestNodes.size() + " NEIGHBOURS.");

        } catch (Exception e) {
            System.out.println("[ERROR] Error processing FIND_NODE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleResponseNode(Object rawPayload) {
        System.out.println("[DISCOVERY] I received a response from us.");

        NodeListPayload container = EncapsulationUtils.encapsulationListNodes(rawPayload);

        if (container != null) {
            List<NodeInfoPayload> list = container.nodes();

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
            BigInteger remoteId = EncapsulationUtils.encapsulationNodeId(info.nodeId());
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
    }

    private void handlePing(Object payload) {
       try {
           sendMessage(outputStream,new Message(MessageType.PONG, payload, myPeer.getHybridLogicalClock()));
       }catch (Exception e) {
           System.out.println("[ERROR] Fail as send PING:  " + e.getMessage());
       }

    }


    public void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
            logger.info("Connection closed");
        } catch (IOException e) {
            logger.severe("Error closing connection: " + e.getMessage());
        }
    }

}
