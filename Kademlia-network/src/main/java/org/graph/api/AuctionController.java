package org.graph.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.server.Peer;
import java.math.BigDecimal;
import java.util.Map;

public class AuctionController {

    private final Peer peerContext;

    public AuctionController(Javalin app, Peer peer) {
        this.peerContext = peer;
        registerRoutes(app);
    }

    private void registerRoutes(Javalin app) {
        app.get("/api/auctions", this::getAllAuctions);
        app.get("/api/auctions/{auctionId}", this::getByIdAuction);
        app.post("/api/auctions", this::createAuction);
        app.post("/api/auctions/{auctionId}/bids", this::placeBid);
        app.post("/api/auctions/test", this::testCreated);
    }

    private void testCreated(Context ctx) {
        new Thread(() -> {
            try {
                var auctionEngine = peerContext.getNetworkGateway().getAuctionEngine();
                System.out.println("\n[SIMULATION] === INITIALED STRESS TEST ===");

                BigDecimal priceA = new BigDecimal("1000");
                String auctionIdA = auctionEngine.createdLocalAuctions(priceA, peerContext);
                waitForAuctionInLedger(auctionIdA);

                BigDecimal priceB = new BigDecimal("500");
                String auctionIdB = auctionEngine.createdLocalAuctions(priceB, peerContext);
                waitForAuctionInLedger(auctionIdB);

                System.out.println("[SIMULATION] Dispatch 40 bids concurrently...");
                for (int i = 1; i <= 20; i++) {
                    BigDecimal bidA = priceA.add(BigDecimal.valueOf(i * 50));
                    auctionEngine.placeBidRequest(auctionIdA, bidA, peerContext);

                    BigDecimal bidB = priceB.add(BigDecimal.valueOf(i * 25));
                    auctionEngine.placeBidRequest(auctionIdB, bidB, peerContext);

                    Thread.sleep(100);
                }
                System.out.println("[SIMULATION] === STRESS TEST CONCLUSION ===");

            } catch (Exception e) {
                System.err.println("[SIMULATION] Fail catastrophic no teste de stress: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        ctx.status(202).json(Map.of(
                "status", "EXECUTING",
                "message", "The global load test has been initiated in the background. Two auction instances will be forged, followed by 40 massive bids on the network."
        ));
    }

    private void waitForAuctionInLedger(String targetId) throws InterruptedException {
        var engine = peerContext.getNetworkGateway().getAuctionEngine();
        int attempts = 0;

        System.out.print("[SIMULATION] Saving miner the block to the auction [" + targetId.substring(0, 8) + "]...");

        while (attempts < 30) {
            if (engine.getWorldState().containsKey(targetId)) {
                System.out.println(" Successful!");
                return;
            }
            Thread.sleep(1000);
            System.out.print(".");
            attempts++;
        }

        System.out.println(" Timeout out, created auctions!");
    }

    private void createAuction(Context ctx) {
        try {
            CreateAuctionRequest request = ctx.bodyAsClass(CreateAuctionRequest.class);

            if (request.startingPrice == null || request.startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                ctx.status(400).json(Map.of("error", "The initial price must be positive."));
                return;
            }

            String auctionId = peerContext.getNetworkGateway().getAuctionEngine().createdLocalAuctions(request.startingPrice, peerContext);

            ctx.status(202).json(Map.of(
                    "status", "PENDING_MINING",
                    "message", "The transaction to create the auction was submitted to Mempool.",
                    "auctionId", auctionId
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Catastrophic failure in the creation of the auction:" + e.getMessage()));
        }
    }

    private void getByIdAuction(Context ctx) {
        String auctionId = ctx.pathParam("auctionId");
        AuctionState state = peerContext.getNetworkGateway().getAuctionEngine().getWorldState().get(auctionId);

        if (state == null) {
            ctx.status(404).json(Map.of("error", "The auction identifier is not found in the validated General Ledger."));
            return;
        }

        ctx.status(200).json(state);
    }

    private void getAllAuctions(Context ctx) {
        Map<String, AuctionState> ledger = peerContext.getNetworkGateway().getAuctionEngine().getWorldState();

        if (ledger.isEmpty()) {
            ctx.status(204).json(Map.of("message", "There are no crystallized auctions in the local blockchain."));
            return;
        }

        ctx.status(200).json(ledger.values());
    }

    private void placeBid(Context ctx) {
        try {
            String auctionId = ctx.pathParam("auctionId");
            PlaceBidRequest request = ctx.bodyAsClass(PlaceBidRequest.class);

            if (request.bidAmount == null) {
                ctx.status(400).json(Map.of("error", "The mathematical value of the bid was not detected in the payload."));
                return;
            }

            peerContext.getNetworkGateway().getAuctionEngine().placeBidRequest(auctionId, request.bidAmount, peerContext);

            ctx.status(202).json(Map.of(
                    "status", "PENDING_MINING",
                    "message", "The bidding process has been completed and is awaiting cryptographic validation."
            ));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Bidding engine crash: " + e.getMessage()));
        }
    }

    private static class CreateAuctionRequest {
        public String description;
        public BigDecimal startingPrice;
    }

    private static class PlaceBidRequest {
        public BigDecimal bidAmount;
    }
}