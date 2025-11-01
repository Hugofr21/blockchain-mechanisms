package org.graph.domain.application;

import org.graph.infrastructure.p2p.Peer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ServerHandle implements Runnable {
    private ServerSocket server;
    private Logger mLogger;
    private Peer myPeer;

    public ServerHandle(ServerSocket server, Logger logger, Peer peer) {
        this.server = server;
        this.mLogger = logger;
        this.myPeer = peer;
    }

    @Override
    public void run() {
        mLogger.info("Server listening for connections...");

        while (!server.isClosed() && myPeer.getIsRunning()) {
            try {
                Socket socket = server.accept();
                mLogger.info("New connection from: " + socket.getRemoteSocketAddress());

                ConnectionHandler handler = new ConnectionHandler(socket, myPeer, mLogger);
                new Thread(handler).start();
            } catch (Exception e) {
                if (myPeer.getIsRunning()) {
                    mLogger.severe("Error accepting client connection: " + e.getMessage());
                }
            }
        }

        mLogger.info("Server stopped listening");
    }
}