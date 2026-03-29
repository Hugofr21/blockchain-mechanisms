package org.graph.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.graph.domain.entities.block.Block;
import org.graph.server.Peer;
import java.util.List;
import java.util.Map;

public class BlockController {
    private final Peer peerContext;

    public BlockController(Javalin app, Peer peer) {
        this.peerContext = peer;
        registerRoutes(app);
    }

    private void registerRoutes(Javalin app) {
        app.get("/api/blocks", this::getAllBlocks);
        app.get("/api/blocks/{hash}", this::getBlockByHash);
    }

    private void getAllBlocks(Context ctx) {
        var engine = peerContext.getNetworkGateway().getBlockchainEngine();
        var organizer = engine.getBlockOrganizer();

        List<Block> ledger = organizer.getOrderedChain();

        if (ledger.isEmpty()) {
            ctx.status(204).json(Map.of("message", "The blockchain is empty."));
            return;
        }
        ctx.status(200).json(ledger);
    }

    private void getBlockByHash(Context ctx) {
        var engine = peerContext.getNetworkGateway().getBlockchainEngine();
        var organizer = engine.getBlockOrganizer();

        String targetHash = ctx.pathParam("hash");

        Block block = organizer.getBlockByHash(targetHash);

        if (block == null) {
            ctx.status(404).json(Map.of(
                    "error", "Block not found!",
                    "hash", targetHash
            ));
            return;
        }

        ctx.status(200).json(block);
    }
}
