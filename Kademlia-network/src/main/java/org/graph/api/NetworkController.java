package org.graph.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.graph.domain.entities.node.Node;
import org.graph.server.Peer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkController {
    private final Peer peerContext;

    public NetworkController(Javalin app, Peer peer) {
        this.peerContext = peer;
        registerRoutes(app);
    }

    private void registerRoutes(Javalin app) {
        app.get("/api/network/identity", this::showMyPeerInfo);
        app.get("/api/network/neighbors", this::getAllNeighbours);
        app.post("/api/network/shutdown", this::shutDown);
    }

    private void showMyPeerInfo(Context ctx) {
        Node myself = peerContext.getMyself();

        ctx.status(200).json(Map.of(
                "peerId", myself.getNodeId().value().toString(),
                "host", myself.getHost(),
                "port", myself.getPort(),
                "difficulty", myself.getNETWORK_DIFFICULTY()
        ));
    }

    private void getAllNeighbours(Context ctx) {
        List<Node> neighbors = peerContext.getNeighboursManager().getActiveNeighbours();

        if (neighbors.isEmpty()) {
            ctx.status(204).json(Map.of("message", "There are no active neighbors linked to this temporal replica."));
            return;
        }

        List<Map<String, Object>> serializedNeighbors = new ArrayList<>();
        for (Node n : neighbors) {
            serializedNeighbors.add(Map.of(
                    "peerId", n.getNodeId().value().toString(),
                    "host", n.getHost(),
                    "port", n.getPort()
            ));
        }

        ctx.status(200).json(serializedNeighbors);
    }

    private void shutDown(Context ctx) {
        ctx.status(202).json(Map.of(
                "status", "SHUTTING_DOWN",
                "message", "Initiating the graceful closure protocol for the Kademlia node."
        ));

        new Thread(() -> {
            try {
                Thread.sleep(500);
                peerContext.getNeighboursManager().shutdown();
                peerContext.stopPeer();
                System.out.println("[SHUTDOWN] Remotely controlled shutdown completed successfully.");
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}