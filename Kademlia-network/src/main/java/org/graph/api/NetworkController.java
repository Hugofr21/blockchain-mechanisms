package org.graph.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.graph.domain.entities.node.Node;
import org.graph.server.Peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

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
        app.get("/api/network/logs", this::getPeerLogs);
    }

    private void getPeerLogs(Context ctx) {
        Node myself = peerContext.getMyself();
        String logFileName = String.format("%s_%d_peer.log", myself.getHost(), myself.getPort());
        Path logPath = Paths.get(logFileName);

        if (!Files.exists(logPath)) {
            ctx.status(404).json(Map.of("error", "The log file corresponding to this instance does not exist in the local file system."));
            return;
        }

        long offset = ctx.queryParamAsClass("offset", Long.class).getOrDefault(0L);
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(100);

        try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
            long fileLength = raf.length();

            if (offset > fileLength) {
                offset = 0L;
            }

            raf.seek(offset);

            List<String> newLines = new ArrayList<>();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int currentByte;

            while ((currentByte = raf.read()) != -1) {
                if (currentByte == '\n') {
                    newLines.add(buffer.toString(StandardCharsets.UTF_8));
                    buffer.reset();
                    if (newLines.size() == limit) {
                        break;
                    }
                } else if (currentByte != '\r') {
                    buffer.write(currentByte);
                }
            }

            long nextOffset = raf.getFilePointer();

            ctx.status(200).json(Map.of(
                    "fileName", logFileName,
                    "linesReturned", newLines.size(),
                    "nextOffset", nextOffset,
                    "data", newLines
            ));
        } catch (IOException e) {
            ctx.status(500).json(Map.of("error", "Input/output failure during log stream extraction: " + e.getMessage()));
        }
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