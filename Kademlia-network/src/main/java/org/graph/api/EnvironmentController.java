package org.graph.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.graph.adapter.outbound.network.message.auction.AuctionPayload;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.auctions.Bid;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.entities.transaction.TransactionType;
import org.graph.domain.valueobject.utils.HashUtils;
import org.graph.server.Peer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.*;

public class EnvironmentController {
    private final Peer peerContext;

    public EnvironmentController(Javalin app, Peer peer) {
        this.peerContext = peer;
        registerRoutes(app);
    }

    private void registerRoutes(Javalin app) {
        app.post("/api/chaos/sybil", this::simulateSybilAttack);
        app.post("/api/chaos/eclipse", this::simulateEclipseAttack);
        app.post("/api/chaos/duplicate-bids", this::simulateDuplicateBids);
        app.post("/api/chaos/rollback", this::rollbackAnAuctionBidByBid);
        app.post("/api/chaos/stress-bids", this::simulateAuctionOpenAddMultipleBids);
        app.post("/api/chaos/poisoned-block", this::simulateBlockFakeIdentityInjection);
    }

    private void simulateSybilAttack(Context ctx) {
        int acceptedCount = 0;
        int rejectedCount = 0;

        for (int i = 0; i < 50; i++) {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();

                BigInteger fakeId = new BigInteger(256, new Random());
                Node attackNode = new Node("192.168.1." + i, 9000 + i, fakeId, 0, peerContext.getMyself().getNETWORK_DIFFICULTY());

                peerContext.getIsKeysInfrastructure().addNeighborPublicKey(fakeId, pair.getPublic());
                boolean added = peerContext.getRoutingTable().addNode(attackNode, peerContext);

                if (added) acceptedCount++;
                else rejectedCount++;
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "Attack generation failed: " + e.getMessage()));
                return;
            }
        }

        boolean success = acceptedCount == 0;
        ctx.json(Map.of(
                "attack", "SYBIL",
                "acceptedNodes", acceptedCount,
                "rejectedNodes", rejectedCount,
                "status", success ? "DEFENSE_OPERATIONAL" : "ROUTING_TABLE_COMPROMISED"
        ));
    }

    private void simulateEclipseAttack(Context ctx) {
        String attackIp = "10.0.0.125";
        int acceptedCount = 0;
        int rejectedCount = 0;
        int difficulty = peerContext.getMyself().getNETWORK_DIFFICULTY();

        for (int i = 0; i < 10; i++) {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                PublicKey publicKey = pair.getPublic();

                long nonce = 0;
                BigInteger target = BigInteger.ONE.shiftLeft(256 - difficulty);
                BigInteger nodeId;

                while (true) {
                    nodeId = HashUtils.calculateHashFromNodeId(publicKey, nonce);
                    if (nodeId.compareTo(target) < 0) break;
                    nonce++;
                }

                Node attackNode = new Node(attackIp, 8000 + i, nodeId, nonce, difficulty);
                peerContext.getIsKeysInfrastructure().addNeighborPublicKey(nodeId, publicKey);
                boolean added = peerContext.getRoutingTable().addNode(attackNode, peerContext);

                if (added) acceptedCount++;
                else rejectedCount++;
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "Eclipse attack failed:" + e.getMessage()));
                return;
            }
        }

        boolean success = acceptedCount <= 2;
        ctx.json(Map.of(
                "attack", "ECLIPSE",
                "acceptedNodes", acceptedCount,
                "rejectedNodes", rejectedCount,
                "status", success ? "SPATIAL_RESTRICTION_OPERATIONAL" : "NODE_ISOLATED"
        ));
    }

    private void simulateDuplicateBids(Context ctx) {
        ChaosTargetRequest req = validateAndExtractTarget(ctx);
        if (req == null) return;

        new Thread(() -> {
            try {
                var auctionEngine = peerContext.getNetworkGateway().getAuctionEngine();
                Bid maliciousBid = new Bid(req.auctionId, new BigDecimal("1500"), System.currentTimeMillis(), peerContext.getMyself().getNodeId().value());
                AuctionPayload payload = AuctionPayload.bid(maliciousBid);
                long nonce = auctionEngine.reserveNextNonce(peerContext.getMyself().getNodeId().value());

                Transaction maliciousTx = new Transaction(
                        TransactionType.BID, peerContext.getIsKeysInfrastructure().getOwnerPublicKey(),
                        payload, peerContext.getMyself().getNodeId().value(), nonce, peerContext.getHybridLogicalClock().getPhysicalClock()
                );

                maliciousTx.setSignature(peerContext.getIsKeysInfrastructure().signMessage(maliciousTx.getDataSign()));

                for (int i = 0; i < 10; i++) {
                    peerContext.getNetworkGateway().getBlockchainEngine().submitTransaction(maliciousTx);
                }
            } catch (Exception e) {
                peerContext.getLogger().severe("Simulation failure in replication: " + e.getMessage());
            }
        }).start();

        ctx.status(202).json(Map.of("status", "INJECTED", "message", "10 duplicate transactions sent to Mempool."));
    }

    private void rollbackAnAuctionBidByBid(Context ctx) {
        ChaosTargetRequest req = validateAndExtractTarget(ctx);
        if (req == null) return;

        var auctionEngine = peerContext.getNetworkGateway().getAuctionEngine();
        AuctionState state = auctionEngine.getWorldState().get(req.auctionId);

        if (state == null) {
            ctx.status(404).json(Map.of("error", "Target auction not found on the local Ledger."));
            return;
        }

        new Thread(() -> {
            try {
                BigDecimal initialBid = state.getCurrentHighestBid().add(new BigDecimal("100"));
                auctionEngine.placeBidRequest(req.auctionId, initialBid, peerContext);
                Thread.sleep(3000);

                BigDecimal secondBid = initialBid.add(new BigDecimal("200"));
                auctionEngine.placeBidRequest(req.auctionId, secondBid, peerContext);
                Thread.sleep(3000);

                synchronized (state) {
                    Set<Bid> history = state.getBidHistory();
                    if (history.size() > 1) {
                        List<Bid> sortedHistory = new ArrayList<>(history);
                        sortedHistory.sort(Comparator.comparingLong(Bid::throwTimestamp));
                        history.remove(sortedHistory.get(sortedHistory.size() - 1));
                    }
                }

                auctionEngine.placeBidRequest(req.auctionId, initialBid.add(new BigDecimal("50")), peerContext);
            } catch (Exception e) {
                peerContext.getLogger().severe("Naive Rollback Failure: " + e.getMessage());
            }
        }).start();

        ctx.status(202).json(Map.of("status", "EXECUTING", "message", "State corruption simulation initiated."));
    }

    private void simulateAuctionOpenAddMultipleBids(Context ctx) {
        ChaosTargetRequest req = validateAndExtractTarget(ctx);
        if (req == null) return;

        var auctionEngine = peerContext.getNetworkGateway().getAuctionEngine();
        AuctionState state = auctionEngine.getWorldState().get(req.auctionId);

        if (state == null) {
            ctx.status(404).json(Map.of("error", "Auction not found on Ledger."));
            return;
        }

        new Thread(() -> {
            BigDecimal nextBidValue = state.getCurrentHighestBid().add(new BigDecimal("10.00"));
            for (int i = 1; i <= 40; i++) {
                try {
                    auctionEngine.placeBidRequest(req.auctionId, nextBidValue, peerContext);
                    nextBidValue = nextBidValue.add(new BigDecimal("10.00"));
                    Thread.sleep(50);
                } catch (Exception e) {
                    peerContext.getLogger().severe("Failure in the bidding process: " + e.getMessage());
                    break;
                }
            }
        }).start();

        ctx.status(202).json(Map.of("status", "EXECUTING", "message", "A flurry of 40 tenders has begun."));
    }

    private void simulateBlockFakeIdentityInjection(Context ctx) {
        new Thread(() -> {
            try {
                List<Node> neighbors = peerContext.getNeighboursManager().getActiveNeighbours();
                if (neighbors.isEmpty()) return;

                Node targetNeighbor = neighbors.get(0);
                Block poisonedBlock = new Block(
                        1, peerContext.getNetworkGateway().getBlockchainEngine().getBlockOrganizer().getChainHeight() + 1,
                        "00631425ea719c4f88f090c3f1f079c98ae9ceb6215813e779ef3cbe071b981621",
                        new ArrayList<>(), peerContext.getMyself().getNETWORK_DIFFICULTY()
                );
                poisonedBlock.setCurrentHash("00f72c1bc26f96f44374911f6e8d6ab4a8b5e43c321136dbd83edb6e71acdf51c7");

                Message attackMsg = new Message(MessageType.BLOCK, poisonedBlock, peerContext.getHybridLogicalClock());
                peerContext.getMkademliaNetwork().sendRPCAsync(targetNeighbor, attackMsg);
            } catch (Exception e) {
                peerContext.getLogger().severe("Poisoned Block attack fails: " + e.getMessage());
            }
        }).start();

        ctx.status(202).json(Map.of("status", "INJECTED", "message", "Poisoned block fired at the first active neighbor."));
    }

    private ChaosTargetRequest validateAndExtractTarget(Context ctx) {
        try {
            ChaosTargetRequest req = ctx.bodyAsClass(ChaosTargetRequest.class);
            if (req.auctionId == null || req.auctionId.isBlank()) {
                ctx.status(400).json(Map.of("error", "The 'auctionId' parameter is required in the JSON body."));
                return null;
            }
            return req;
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid JSON payload."));
            return null;
        }
    }

    private static class ChaosTargetRequest {
        public String auctionId;
    }
}