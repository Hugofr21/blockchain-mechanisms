package org.graph.infrastructure.p2p;


import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.domain.entities.p2p.NodeId;
import org.graph.domain.utils.CryptoUtils;
import org.graph.infrastructure.network.message.HandshakePayload;
import org.graph.infrastructure.utils.MessageUtils;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.PublicKey;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static org.graph.infrastructure.utils.Constants.*;
import static org.graph.infrastructure.utils.MessageUtils.readMessage;
import static org.graph.infrastructure.utils.MessageUtils.sendMessage;

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


    @Override
    public void run() {
        try {
            initStreams();
            logger.info("Connection loop started: " + socket.getRemoteSocketAddress());

            while (myPeer.getIsRunning() && !socket.isClosed() && running) {
                try {
                    Message message = readMessage(inputStream);

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

    private void handleFindNode(Object payload) {
        try {

            BigInteger targetId = (BigInteger) payload;

            List<Node> closestNodes = myPeer.getRoutingTable().findClosestNodesProximity(targetId, NODE_K);

            Message response = new Message(MessageType.ACK, closestNodes, myPeer.getHybridLogicalClock());

            sendMessage(outputStream,response);

        }catch (Exception e) {
            System.out.println("[ERROR] Deserialized target id:  " + e.getMessage());
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


    private void closeConnection() {
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

    public DataInputStream getInputStream() {
        if (inputStream == null) return null;
        return inputStream;
    }
}
