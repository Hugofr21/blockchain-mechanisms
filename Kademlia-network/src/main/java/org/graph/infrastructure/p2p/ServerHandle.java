package org.graph.infrastructure.p2p;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerHandle implements Runnable {
    private ServerSocket server;
    private Logger mLogger;
    private Peer myPeer;
    private final ExecutorService connectionPool;

    public ServerHandle(ServerSocket server, Logger logger, Peer peer) {
        this.server = server;
        this.mLogger = logger;
        this.myPeer = peer;
        this.connectionPool = Executors.newFixedThreadPool(50);
    }

    @Override
    public void run() {
        mLogger.info("Server listening for connections...");

        while (!server.isClosed() && myPeer.getIsRunning()) {
            try {
                Socket socket = server.accept();
                mLogger.info("New connection from: " + socket.getRemoteSocketAddress());

                connectionPool.execute(() -> {
                    try {
                        ConnectionHandler handler = new ConnectionHandler(socket, myPeer, mLogger);
                        // O handshake inicial deve ocorrer dentro do handler.
                        // Apenas após validar o ID e o Nonce o nó deve ser adicionado ao manager.
                        if (handler.performHandshake()) {
                            myPeer.getNeighboursManager().addConnection(myPeer.getMyself(),handler);
                        } else {
                            socket.close();
                        }
                    } catch (Exception e) {
                        mLogger.log(Level.WARNING, "Error handling connection", e);
                    }
                });

            } catch (Exception e) {
                if (myPeer.getIsRunning()) {
                    mLogger.severe("Error accepting client connection: " + e.getMessage());
                }
            }
        }

        mLogger.info("Server stopped listening");
    }
    private void shutdown() {
        connectionPool.shutdownNow();
        mLogger.info("Server stopped listening and pool shutdown");
    }
}