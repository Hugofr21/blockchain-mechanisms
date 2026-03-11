package org.graph.infrastructure.network;

import org.graph.domain.entities.node.Node;
import org.graph.adapter.inbound.network.Handshake;
import org.graph.server.Peer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ServerHandle implements Runnable {
    private ServerSocket server;
    private Logger mLogger;
    private Peer myPeer;
    private final ExecutorService connectionPool;
    private static final int HANDSHAKE_TIMEOUT_MS = 5000;

    public ServerHandle(ServerSocket server, Logger logger, Peer peer) {
        this.server = server;
        this.mLogger = logger;
        this.myPeer = peer;
        this.connectionPool = Executors.newFixedThreadPool(50);
    }

    @Override
    public void run() {
        mLogger.info("Server listening for connections on port " + server.getLocalPort());

        while (!server.isClosed() && myPeer.getIsRunning()) {
            try {
                Socket socket = server.accept();
                mLogger.info("Incoming connection from: " + socket.getRemoteSocketAddress());

                connectionPool.execute(() -> handleIncomingSocket(socket));

            } catch (Exception e) {
                if (myPeer.getIsRunning()) {
                    mLogger.severe("Critical error in server accept loop: " + e.getMessage());
                }
            }
        }
        mLogger.info("Server stopped listening");
    }

    private void handleIncomingSocket(Socket socket) {
        boolean success = false;
        try {

            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);

            ConnectionHandler handler = new ConnectionHandler(socket, myPeer, mLogger);

            handler.initStreams();

            Optional<Node> handshake = Handshake.doHandshake(
                    myPeer,
                    handler
            );

            if (handshake.isPresent()) {
                Node remoteNode = handshake.get();
                handler.setRemoteNode(remoteNode);

                socket.setSoTimeout(0);

                myPeer.getRoutingTable().addNode(remoteNode, myPeer);

                myPeer.getNeighboursManager().addConnection(remoteNode, handler);
                success = true;
            } else {
                mLogger.warning("Handshake rejected from " + socket.getRemoteSocketAddress());
            }

        } catch (SocketTimeoutException e) {
            mLogger.warning("Handshake timed out for " + socket.getRemoteSocketAddress());
        } catch (Exception e) {
            mLogger.warning("Error handling connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (!success) {
                try {
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException e) { /* ignorar */ }
            }
        }
    }

    public void shutdown() {
        try {
            if (server != null && !server.isClosed()) server.close();
            connectionPool.shutdownNow();
        } catch (IOException e) {
            mLogger.warning("Error shutting down server: " + e.getMessage());
        }
    }
}