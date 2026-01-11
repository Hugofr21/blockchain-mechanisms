package org.graph.infrastructure.p2p;

import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.network.message.HandshakePayload;
import org.graph.infrastructure.utils.Base64Utils;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import static org.graph.infrastructure.utils.Constants.NODE_K;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private Peer myPeer;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Logger logger;
    private volatile boolean running;

    public ConnectionHandler(Socket socket, Peer myPeer, Logger mLogger) {
        this.socket = socket;
        this.myPeer = myPeer;
        this.logger = mLogger;
        this.running = true;
    }


    @Override
    public void run() {
        try {
            outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            logger.info("Connection established with: " + socket.getRemoteSocketAddress());

            while (myPeer.getIsRunning() && !socket.isClosed() && running) {
                try {
                    int length = inputStream.readInt();

                    byte[] receivedBytes = new byte[length];
                    inputStream.readFully(receivedBytes);

                    String receivedBase64 = new String(receivedBytes, "UTF-8");
                    byte[] objectBytes = Base64Utils.decodeToBytes(receivedBase64);

                    Object obj = SerializationUtils.deserialize(objectBytes);


                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        handleMessage(message); // Passa o objeto completo
                    } else {
                        logger.warning("Objeto recebido não é do tipo Message");
                    }

                } catch (EOFException e) {
                    logger.info("Connection closed by peer");
                    break;
                } catch (IOException e) {
                    logger.severe("I/O error: " + e.getMessage());
                    break;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            logger.severe("Connection error: " + e.getMessage());
        } finally {
            closeConnection();
        }
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

    private void handleAck(byte[] payload) {
    }

    private void handleRequest(byte[] payload) {
    }

    private void handleChunk(byte[] payload) {
    }

    private void handleBlock(byte[] payload) {
    }

    private void handleStorage(byte[] payload) {
    }

    private void handleFindValue(byte[] payload) {
    }

    private void handleFindNode(byte[] payload) {
        try {
            BigInteger targetId = (BigInteger) SerializationUtils.deserialize(payload);
            List<Node> closestNodes = myPeer.getRoutingTable().findClosestNodesProximity(targetId, NODE_K);

            Message response = new Message(MessageType.ACK, closestNodes);
            sendMessage(response);

        }catch (Exception e) {
            System.out.println("[ERROR] Deserialized target id:  " + e.getMessage());
        }

    }

    private void handlePong(byte[] payload) {
    }

    private void handlePing(byte[] payload) {
       try {
           sendMessage(new Message(MessageType.PONG, payload));
       }catch (Exception e) {
           System.out.println("[ERROR] Fail as send PING:  " + e.getMessage());
       }

    }


    public byte[] receiveBytes() throws IOException {
        byte[] sizeBytes = inputStream.readNBytes(4);
        int size = ByteBuffer.wrap(sizeBytes).getInt();

        byte[] data = new byte[size];
        int offset = 0;
        int chunkSize = 65536;
        while (offset < size) {
            int len = Math.min(chunkSize, size - offset);
            int read = inputStream.read(data, offset, len);
            if (read == -1) throw new EOFException("End of stream reached unexpectedly");
            offset += read;
        }
        logger.info("Received " + size + " bytes");
        return data;
    }


    public void sendMessage(Message message) throws IOException {
        byte[] payloadBytes = message.getPayload();

        outputStream.write(message.getType().getCode());
        outputStream.write(ByteBuffer.allocate(4).putInt(payloadBytes.length).array());
        outputStream.write(payloadBytes);
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
        return true;
    }

    private boolean validateIdentity(Node node, HandshakePayload proof) {
        return true;
    }
}
