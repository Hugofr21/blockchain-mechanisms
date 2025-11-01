package org.graph.domain.application;

import org.graph.domain.entities.p2p.Peer;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private Peer myPeer;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
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
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            logger.info("Connection established with: " + socket.getRemoteSocketAddress());

            while (myPeer.getIsRunning() && !socket.isClosed() && running) {
                try {
                    Message message = (Message) inputStream.readObject();
                    processEvent(message);
                } catch (EOFException e) {
                    logger.info("Connection closed by peer");
                    break;
                } catch (ClassNotFoundException e) {
                    logger.severe("Unknown message type received: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("Connection error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }


    private void processEvent(Message message) {
        MessageType type = message.getType();
        switch (type) {

        }

    }

    public void sendMessage(Message message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            logger.severe("Error sending message: " + e.getMessage());
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
}
