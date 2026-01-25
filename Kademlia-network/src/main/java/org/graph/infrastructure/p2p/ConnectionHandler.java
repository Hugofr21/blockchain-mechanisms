package org.graph.infrastructure.p2p;


import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.domain.entities.p2p.NodeId;
import org.graph.domain.utils.CryptoUtils;
import org.graph.infrastructure.network.message.HandshakePayload;
import org.graph.infrastructure.utils.Constants;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.PublicKey;
import java.util.List;
import java.util.logging.Logger;

import static org.graph.infrastructure.utils.Constants.*;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private Peer myPeer;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Logger logger;
    private volatile boolean running;
    private Node remoteNode;

    public ConnectionHandler(Socket socket, Peer myPeer, Logger mLogger) {
        this.socket = socket;
        this.myPeer = myPeer;
        this.logger = mLogger;
        this.running = true;
    }


    public Node getRemoteNode() {return remoteNode;}


    @Override
    public void run() {
        try {
            initStreams();
            logger.info("Connection loop started: " + socket.getRemoteSocketAddress());

            while (myPeer.getIsRunning() && !socket.isClosed() && running) {
                try {
                    Message message = readMessage();

                    if (message.getType() == MessageType.HELLO) {
                        logger.info("Redundant HELLO received in loop.");
                        continue;
                    }

                    handleMessage(message);

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

    private void initStreams() throws IOException {
        if (outputStream == null) outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        if (inputStream == null) inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    private void handleMessage(Message message) {
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

            Message response = new Message(MessageType.ACK, closestNodes);

            sendMessage(response);

        }catch (Exception e) {
            System.out.println("[ERROR] Deserialized target id:  " + e.getMessage());
        }

    }

    private void handlePong(Object payload) {
    }

    private void handlePing(Object payload) {
       try {
           sendMessage(new Message(MessageType.PONG, payload));
       }catch (Exception e) {
           System.out.println("[ERROR] Fail as send PING:  " + e.getMessage());
       }

    }


    /**
     * Write the deserializer.
     */
    private Message readMessage() throws IOException, ClassNotFoundException {
        int length = inputStream.readInt();
        if (length <= 0 || length > MAX_MESSAGE_SIZE * 2)
            throw new IOException("Invalid size: " + length);

        byte[] raw = new byte[length];
        inputStream.readFully(raw);
        Object obj = SerializationUtils.deserialize(raw);

        if (!(obj instanceof Message))
            throw new IOException("Expected Message, got " + obj.getClass());

        return (Message) obj;
    }

    public synchronized void sendMessage(Object object) throws IOException {
        if (socket.isClosed()) throw new IOException("Socket closed");
        byte[] data = SerializationUtils.serialize(object);
        outputStream.writeInt(data.length);
        outputStream.write(data);
        outputStream.flush();
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

    public boolean performHandshake() {
        try {
            initStreams();
            socket.setSoTimeout(5000);

            long timestamp = System.currentTimeMillis();
            String challengeData = myPeer.getMyself().getNodeId().value().toString() + ":" + timestamp;
            byte[] signature = myPeer.getIsKeysInfrastructure().signMessage(challengeData);

            HandshakePayload myPayload = new HandshakePayload(
                    myPeer.getMyself(),
                    myPeer.getIsKeysInfrastructure().getOwnerPublicKey(),
                    timestamp,
                    signature
            );


            sendMessage(new Message(MessageType.HELLO, myPayload));

            Message response = readMessage();
            if (response == null || response.getType() != MessageType.HELLO) {
                logger.warning("Invalid Handshake Response: " + (response == null ? "null" : response.getType()));
                return false;
            }


            Object rawPayload = response.getPayload();
            HandshakePayload remotePayload;

            if (rawPayload instanceof HandshakePayload) {

                remotePayload = (HandshakePayload) rawPayload;
            }
            else if (rawPayload instanceof byte[]) {

                remotePayload = (HandshakePayload) SerializationUtils.deserialize((byte[]) rawPayload);
            }
            else {
                logger.severe("Tipo de payload desconhecido no Handshake: " + rawPayload.getClass().getName());
                return false;
            }

            if (!validateIdentity(remotePayload)) {
                return false;
            }


            this.remoteNode = remotePayload.node();

            try {
                PublicKey pk = (PublicKey) remotePayload.publicKey();
                myPeer.getIsKeysInfrastructure().addNeighborPublicKey(
                        this.remoteNode.getNodeId().value(),
                        pk
                );
            } catch (Exception e) {
                logger.warning("Could not save neighbor key: " + e.getMessage());
            }

            socket.setSoTimeout(0);
            return true;

        } catch (Exception e) {
            logger.severe("Handshake Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private boolean validateIdentity(HandshakePayload payload) {
        try {
            Node node = payload.node();
            PublicKey receivedKey = (PublicKey) payload.publicKey();

            if (receivedKey == null) {
                logger.severe("[SECURITY FAIL] Payload sem Chave Pública!");
                return false;
            }


            NodeId calculatedId = NodeId.createFromProof(
                    receivedKey,
                    node.getNonce(),
                    NETWORK_DIFFICULTY
            );
            if (!calculatedId.equals(node.getNodeId())) {
                logger.severe("SPOOFING DETECTED: ID does not match Public Key");
                return false;
            }


            String challengeData = node.getNodeId().value().toString() + ":" + payload.timestamp();

            return CryptoUtils.verifySignature(
                    receivedKey,
                    challengeData,
                    payload.signature()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
