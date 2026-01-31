package org.graph.adapter.p2p;


import org.graph.adapter.network.kademlia.Handshake;
import org.graph.adapter.network.message.node.FindNodePayload;
import org.graph.adapter.network.message.node.NodeInfoPayload;
import org.graph.adapter.network.message.node.NodeListPayload;
import org.graph.adapter.utils.Base64Utils;
import org.graph.adapter.utils.EncapsulationUtils;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.adapter.utils.SerializationUtils;
import org.graph.gateway.NetworkGateway;

import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static org.graph.adapter.utils.Constants.*;
import static org.graph.adapter.utils.MessageUtils.readMessage;
import static org.graph.adapter.utils.MessageUtils.sendMessage;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private Peer myPeer;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Logger logger;
    private volatile boolean running;
    private Node remoteNode;
    private final ConcurrentMap<UUID, CompletableFuture<Message>> pendingResponses;

    public ConnectionHandler(Socket socket, Peer myPeer, Logger mLogger) {
        this.socket = socket;
        this.myPeer = myPeer;
        this.logger = mLogger;
        this.running = true;
        this.pendingResponses = new ConcurrentHashMap<>();
    }

    public Peer getPeer() { return myPeer;}
    public Node getRemoteNode() {return remoteNode;}
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

                    dispatch(message);

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

    private void dispatch(Message message) {
        switch (message.getType()) {
            case PING -> handlePing(message.getPayload());
            case PONG -> handlePong(message.getPayload());
            case FIND_NODE -> handleFindNode(message.getPayload());
            case FIND_VALUE -> handleFindValue(message.getPayload());
            case STORAGE -> handleStorage(message.getPayload());
            case BLOCK -> handleBlock(message.getPayload());
            case CHUNK -> handleChunk(message.getPayload());
            case REQUEST -> handleRequest(message.getPayload());
            case RESPONSE_NODES -> handleResponseNode(message.getPayload());
            case ACK -> handleAck(message.getPayload());

            default -> logger.warning("Unhandled message type: " + message.getType());
        }
    }


    private void handleAck(Object payload) {

    }

    private void handleRequest(Object payload) {
    }

    private void handleChunk(Object payload) {
    }

    private void handleBlock(Object payload) {
        try {
            byte[] raw = (byte[]) payload;
            Block block = (Block) SerializationUtils.deserialize(raw);
            // isValidBlock

        }catch (Exception e) {
            System.out.println("Exception in handleBlock: " + e.getMessage());
        }
    }

    private void handleStorage(Object payload) {
    }

    private void handleFindValue(Object payload) {

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

    public void setRemoteNode(Node bootstrapNode) {
        remoteNode = bootstrapNode;
    }

}
