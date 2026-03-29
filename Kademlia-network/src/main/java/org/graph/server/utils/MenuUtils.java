package org.graph.server.utils;


import org.graph.adapter.outbound.network.message.auction.AuctionPayload;
import org.graph.domain.entities.auctions.Bid;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.entities.transaction.TransactionType;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.domain.valueobject.utils.HashUtils;
import org.graph.server.Peer;
import org.graph.domain.entities.node.Node;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.*;

public class MenuUtils {

    private static final Scanner scanner = new Scanner(System.in);

    public static void printMenu(String[] args) {
        System.out.println("\nMenu:");
        if (args.length == 0) {
            System.out.println("To initiate a peer relationship, it must contain: <port>");
            return;
        }

    }


    public static void showMainMenu(Peer peer) {
        while (true) {
            System.out.println("\n=== Menu Principal ===");
            System.out.println("1) Display about this peer");
            System.out.println("2) Show the NEIGHBOUR relationship.");
            System.out.println("3) Show the list of BLOCKCHAIN.");
            System.out.println("4) Auction Market (Create/Bid).");
            System.out.println("5) Environment the test (Nodes/Object).");
            System.out.println("6) Exit");

            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1: showMyPeerInfo(peer); break;
                case 2: showNeighboursMenu(peer);break;
                case 3: showBlockchainMenu(peer);break;
                case 4: showAuctionMenu(peer);break;
                case 5: environmentTest(peer); break;
                case 6: shutDown(peer); return;
                default: System.out.println("Option invalid!!");
            }
        }
    }

    private static void shutDown(Peer peer) {
        System.out.println("Shutting down...");
        peer.getNeighboursManager().shutdown();
        peer.stopPeer();
        System.out.println("Shut down complete!");
        System.exit(0);

    }

    private static void environmentTest(Peer peer) {
        System.out.println("\n=== Security Test Environment ===");
        System.out.println("1) Simulate Sybil Attack (Fake Identity Injection)");
        System.out.println("2) Simulate Eclipse Attack (IP Saturation)");
        System.out.println("3) Security Test: Simulate Duplicate Bids (Replay Attack)");
        System.out.println("4) Rollback an auction bid by bid.");
        System.out.println("5) Auction open, add multiple bids.");
        System.out.println("6) Simulate Block (Fake Identity Injection).");
        System.out.println("0) Return");
        System.out.print("Choose the attack vector: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1: simulateSybilAttack(peer); break;
            case 2: simulateEclipseAttack(peer); break;
            case 3: simulateDuplicateBidAttack(peer); break;
            case 4: simulateNaiveRollbackAttack(peer); break;
            case 5: simulateAuctionOpenMultipleBids(peer); break;
            case 6: simulateAttackBlock(peer); break;
            case 0: return;
            default: System.out.println("Invalid option.");

        }
    }

    private static void simulateAttackBlock(Peer peer) {
        System.out.println("\n[SIMULATION] Starting Poisoned Block Attack (Acting as the Attacker)...");

        new Thread(() -> {
            try {

                List<Node> neighbors = peer.getNeighboursManager().getActiveNeighbours();
                if (neighbors.isEmpty()) {
                    System.err.println("[SIMULATION] Error: No active neighbors found. Connect to a network first.");
                    return;
                }


                Node targetNeighbor = neighbors.getFirst();


                System.out.println("[SIMULATION] Forging poisoned block...");
                Block poisonedBlock = new Block(
                        1,
                        peer.getNetworkGateway().getBlockchainEngine().getBlockOrganizer().getChainHeight() + 1,
                        "00631425ea719c4f88f090c3f1f079c98ae9ceb6215813e779ef3cbe071b981621",
                        new ArrayList<>(),
                        peer.getMyself().getNETWORK_DIFFICULTY()
                );

                poisonedBlock.setCurrentHash("00f72c1bc26f96f44374911f6e8d6ab4a8b5e43c321136dbd83edb6e71acdf51c7");



                System.out.println("[SIMULATION] Dispatching poisoned block via secure tunnel to neighbor on port: " + targetNeighbor.getPort());

                Message attackMsg = new Message(
                        MessageType.BLOCK,
                        poisonedBlock,
                        peer.getHybridLogicalClock()
                );


                peer.getMkademliaNetwork().sendRPCAsync(targetNeighbor, attackMsg);


                System.out.println("\n[SIMULATION COMPLETED] Payload delivered.");
                System.out.println(" ARCHITECTURAL NOTE ON METRICS:");
                System.out.println(" Since your node originated the attack, your local dashboard will remain unchanged.");
                System.out.println(" To validate the defense, observe the logs or the Grafana of the victim node (" + targetNeighbor.getPort() + ").");
                System.out.println(" The victim node should cut its reputation (TrustScore) and log 'POISONED_BLOCK'.");

            } catch (Exception e) {
                System.err.println("[CRITICAL ERROR] Simulation failed: " + e.getMessage());
            }
        }).start();
    }

    private static void simulateAuctionOpenMultipleBids(Peer peer) {
        var auctionEngine = peer.getNetworkGateway().getAuctionEngine();
        var ledger = auctionEngine.getWorldState();

        System.out.print("\n[SIMULATION] Enter the auction ID (Hash): ");
        String auctionId = scanner.nextLine().trim();

        AuctionState state = ledger.get(auctionId);
        if (state == null) {
            System.err.println("[ERROR] Auction not found in the local Ledger.");
            return;
        }

        BigDecimal increment = new BigDecimal("10.00");
        BigDecimal nextBidValue = state.getCurrentHighestBid().add(increment);

        System.out.println("[AUCTION_INFO] Starting sequence of 40 bids for: " + auctionId);
        System.out.println("[AUCTION_INFO] Initial simulation value: " + nextBidValue);

        for (int i = 1; i <= 40; i++) {
            try {

                auctionEngine.placeBidRequest(auctionId, nextBidValue, peer);

                System.out.println("[YES #" + i + "] Bid submitted: " + nextBidValue);

                nextBidValue = nextBidValue.add(increment);

                Thread.sleep(50);

            } catch (Exception e) {
                System.err.println("[ERROR SIM #" + i + "] Submission failed: " + e.getMessage());
                break;
            }
        }

        System.out.println("[SIMULATION COMPLETED] 40 transactions sent to Mempool.");
    }

    public static void simulateNaiveRollbackAttack(Peer peer) {
        System.out.println("\n[SIMULATION] Starting State Corruption Test via Naive Rollback...");

        var auctionEngine = peer.getNetworkGateway().getAuctionEngine();
        var ledger = auctionEngine.getWorldState();

        if (ledger.isEmpty()) {
            System.err.println("[ERROR] No active auctions found. Please create an auction first (Menu 4 -> 1).");
            return;
        }

        System.out.print("\nAuction ID (Hash): ");
        String auctionId = scanner.nextLine().trim();


        AuctionState state = ledger.get(auctionId);
        if (state == null) {
            System.err.println("[ERROR] Auction ID not found in the local ledger. Aborting simulation.");
            return;
        }

        BigInteger myNodeId = peer.getMyself().getNodeId().value();

        new Thread(() -> {
            try {
                System.out.println("\n[PHASE 1] Establishing Baseline State for Auction: " + auctionId.substring(0, 8) + "...");
                BigDecimal initialBid = state.getCurrentHighestBid().add(new BigDecimal("100"));
                auctionEngine.placeBidRequest(auctionId, initialBid, peer);


                Thread.sleep(3000);

                BigDecimal secondBid = initialBid.add(new BigDecimal("200"));
                auctionEngine.placeBidRequest(auctionId, secondBid, peer);

                Thread.sleep(3000);

                System.out.println("\n[CURRENT STATE] Highest Bid is now: " + state.getCurrentHighestBid());
                System.out.println("[CURRENT STATE] Expected Next Nonce for user: " + auctionEngine.getExpectedLedgerNonce(myNodeId));

                System.out.println("\n[PHASE 2] Simulating a Fork with Naive Rollback (UNDO)...");
                System.out.println("[WARNING] Attempting to manually undo the last bid and decrement the nonce.");


                synchronized (state) {
                    Set<Bid> history = state.getBidHistory();
                    if (history.size() > 1) {

                        List<Bid> sortedHistory = new ArrayList<>(history);
                        sortedHistory.sort(Comparator.comparingLong(Bid::throwTimestamp));

                        Bid lastBid = sortedHistory.get(sortedHistory.size() - 1);
                        history.remove(lastBid);

                        System.out.println("[NAIVE UNDO] Removed bid of: " + lastBid.bidPrice());

                        System.out.println("[BUG DETECTED] Current Highest Bid remains stuck at: " + state.getCurrentHighestBid() + " (Should have reverted!)");
                    }
                }


                System.out.println("[BUG DETECTED] Nonce state is irreversibly desynchronized. System expects: " + auctionEngine.getExpectedLedgerNonce(myNodeId));

                System.out.println("\n[PHASE 3] System Failure Demonstration...");
                System.out.println("Attempting to place a legitimate new bid after the naive rollback.");

                BigDecimal validBid = initialBid.add(new BigDecimal("50"));
                System.out.println("Placing new bid: " + validBid);

                auctionEngine.placeBidRequest(auctionId, validBid, peer);

                System.out.println("\n[CONCLUSION] The Naive Rollback has corrupted the local ledger. The node is now out of consensus.");

            } catch (Exception e) {
                System.err.println("[CRITICAL ERROR] Simulation failed: " + e.getMessage());
            }
        }).start();
    }

    public static void simulateDuplicateBidAttack(Peer peer) {
        System.out.print("\nAuction ID (Hash): ");
        String auctionId = scanner.nextLine().trim();

        new Thread(() -> {
            try {
                System.out.println("\n[SIMULATION] Starting Attack Vector: Duplicate Submission (Replay Attack) in auction: " + auctionId);

                var auctionEngine = peer.getNetworkGateway().getAuctionEngine();
                BigDecimal maliciousBidValue = new BigDecimal("1500");

               Bid maliciousBid = new Bid(
                        auctionId,
                        maliciousBidValue,
                        System.currentTimeMillis(),
                        peer.getMyself().getNodeId().value()
                );

               AuctionPayload payload = AuctionPayload.bid(maliciousBid);
                long nonce = auctionEngine.reserveNextNonce(peer.getMyself().getNodeId().value());

                Transaction maliciousTx = new Transaction(
                        TransactionType.BID,
                        peer.getIsKeysInfrastructure().getOwnerPublicKey(),
                        payload,
                        peer.getMyself().getNodeId().value(),
                        nonce,
                        peer.getHybridLogicalClock().getPhysicalClock()
                );

                String data = maliciousTx.getDataSign();
                byte[] signature = peer.getIsKeysInfrastructure().signMessage(data);
                maliciousTx.setSignature(signature);

                System.out.println("[SIMULATION] Forged transaction.TxID: " + maliciousTx.getTxId());
                System.out.println("[SIMULATION] Firing 10 identical simultaneous injections...");

                for (int i = 0; i < 10; i++) {
                    System.out.println("[SIMULATION] Injecting duplicate load (Instance " + (i + 1) + ")");
                    peer.getNetworkGateway().getBlockchainEngine().submitTransaction(maliciousTx);
                }

                System.out.println("[SIMULATION] Payload delivered. Waiting for block mining to verify state machine idempotency.");
            } catch (Exception e) {
                System.err.println("[CRITICAL ERROR] Simulation thread failed: " + e.getMessage());
            }
        }).start();
    }

    private static void simulateEclipseAttack(Peer peer) {
        System.out.println("\n[SIMULATION] Starting Eclipse Attack Vector...");
        System.out.println("[SIMULATION] Generating nodes with valid Proof of Work from target IP: 10.0.0.125...");

        String attackIp = "10.0.0.125";
        int acceptedCount = 0;
        int rejectedCount = 0;
        int difficulty = peer.getMyself().getNETWORK_DIFFICULTY();

        for (int i = 0; i < 10; i++) {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                PublicKey publicKey = pair.getPublic();

                long nonce = 0;
                BigInteger nodeId;
                BigInteger target = BigInteger.ONE.shiftLeft(256 - difficulty);

                while (true) {
                    nodeId = HashUtils.calculateHashFromNodeId(publicKey, nonce);
                    if (nodeId.compareTo(target) < 0) {
                        break;
                    }
                    nonce++;
                }

                System.out.println("[SIMULATION] Forged identity " + i + " successfully mined (Nonce: " + nonce + ")");


                Node attackNode = new Node(
                        attackIp,
                        8000 + i,
                        nodeId,
                        nonce,
                        difficulty
                );


                peer.getIsKeysInfrastructure().addNeighborPublicKey(nodeId, publicKey);

                boolean added = peer.getRoutingTable().addNode(attackNode, peer);

                if (added) {
                    acceptedCount++;
                } else {
                    rejectedCount++;
                }

            } catch (Exception e) {
                System.err.println("[CRITICAL ERROR] Attack generator iteration failed: " + e.getMessage());
            }
        }


        System.out.println("\n=== Security Assessment Report ===");
        System.out.println("Hostile nodes admitted to the table: " + acceptedCount);
        System.out.println("Hostile nodes rejected (IP Filter): " + rejectedCount);

        if (acceptedCount <= 2) {
            System.out.println("[SUCCESS] Eclipse defense operational. The topology prevented monopolization of the KBucket.");
        } else {
            System.err.println("[SERIOUS FAILURE] The spatial restriction failed. The attacker isolated the node from the legitimate network.");
        }
    }

    private static void simulateSybilAttack(Peer peer) {
        System.out.println("\n[SIMULATION] Starting Sybil Attack Vector...");
        System.out.println("[SIMULATION] Attempting to inject 50 forged cryptographic identities (without Proof of Work)...");

        int acceptedCount = 0;
        int rejectedCount = 0;

        for (int i = 0; i < 50; i++) {
            try {
                KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();

                java.math.BigInteger fakeId = new java.math.BigInteger(256, new java.util.Random());
                long invalidNonce = 0;

                org.graph.domain.entities.node.Node attackNode = new Node(
                        "192.168.1." + i,
                        9000 + i,
                        fakeId,
                        invalidNonce,
                        peer.getMyself().getNETWORK_DIFFICULTY()
                );

                peer.getIsKeysInfrastructure().addNeighborPublicKey(fakeId, pair.getPublic());

                boolean added = peer.getRoutingTable().addNode(attackNode, peer);

                if (added) {
                    acceptedCount++;
                } else {
                    rejectedCount++;
                }
            } catch (Exception e) {
                System.err.println("[CRITICAL ERROR] Attack generator iteration failed: " + e.getMessage());
            }
        }

        System.out.println("\n=== Security Assessment Report (Sybil) ===");
        System.out.println("Hostile nodes admitted to the table: " + acceptedCount);
        System.out.println("Hostile nodes rejected (PoW Filter): " + rejectedCount);

        if (acceptedCount == 0) {
            System.out.println("[SUCCESS] Sybil defense operational. The infrastructure blocked all forged identities.");
        } else {
            System.err.println("[SERIOUS FAILURE] Mathematical validation failed. The attacker polluted the routing table.");
        }
    }


    private static void showMyPeerInfo(Peer peer) {
        System.out.println("\n=== Info of Peer ===");
        System.out.println("My self: " + peer.getMyself());
    }


    private static void showNeighboursMenu(Peer peer) {
        System.out.println("\n=== Kademlia DHT & Network Menu ===");
        System.out.println("1) List Active Connected Neighbours");
        System.out.println("2) DHT: Find Node (Lookup K-Closest)");
        System.out.println("3) DHT: Store Value (Publish Data)");
        System.out.println("4) DHT: Find Value (Search Data)");
        System.out.println("5) DHT: List Local Storage (Debug History)");
        System.out.println("0) Back");

        System.out.print("Choose a option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                System.out.println("List of NEIGHBOUR relationships: ");
                List<Node> neighbors = peer.getNeighboursManager().getActiveNeighbours();
                if (neighbors.isEmpty()){
                    System.out.println("Next moment not found neighbour.");
                }else {
                    System.out.printf("%-15s | %-6s | %s%n", "IP", "Port", "ID (Short)");
                    System.out.println("------------------------------------------------");
                    for (Node n : neighbors) {
                        String shortId = n.getNodeId().value().toString();
                        if (shortId.length() > 15) shortId = shortId.substring(0, 15) + "...";

                        System.out.printf("%-15s | %-6d | %s%n",
                                n.getHost(),
                                n.getPort(),
                                shortId
                        );
                    }
                    System.out.println("------------------------------------------------");
                    System.out.println("Total: " + neighbors.size() + " neighbour.");
                }
                break;
            case 2:
                System.out.print("Digit the ID of node: ");
                BigInteger nodeId = scanner.nextBigInteger();
                List<Node> closest = peer.getMkademliaNetwork().findNode(nodeId);
                System.out.println("\n[DHT] Result - K-Closest Nodes found:");
                if (closest.isEmpty()) {
                    System.out.println(" >> No nodes found.");
                } else {

                    System.out.printf("%-15s | %-6s | %-15s | %s%n", "Host", "Port", "ID (Short)", "XOR Distance");
                    System.out.println("---------------------------------------------------------------");

                    for (Node n : closest) {

                        BigInteger dist = n.getNodeId().distanceBetweenNode(nodeId);

                        String shortId = n.getNodeId().value().toString();
                        if (shortId.length() > 10) shortId = shortId.substring(0, 10) + "...";

                        String shortDist = dist.toString();
                        if (shortDist.length() > 10) shortDist = shortDist.substring(0, 10) + "...";

                        System.out.printf("%-15s | %-6d | %-15s | %s%n",
                                n.getHost(),
                                n.getPort(),
                                shortId,
                                shortDist
                        );
                    }
                }

                System.out.println("Functionality not implemented..");
                break;
            case 3:
                System.out.println("Neighbor with the most relationships:: ?");
                break;
            case 4:
                System.out.println("Find Value");

                break;
            case 5:
                System.out.println("\n=== Local DHT Storage Content ===");
                Map<String, String> dataMap = peer.getMkademliaNetwork().getStorage().getAllDataSnapshot();

                if (dataMap.isEmpty()) {
                    System.out.println("Storage is empty.");
                } else {
                    System.out.printf("%-64s | %s%n", "Key (Hex)", "Value Summary");
                    System.out.println("----------------------------------------------------------------------------------");

                    for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                        System.out.printf("%-64s | %s%n",
                                entry.getKey(),
                                entry.getValue()
                        );
                    }
                    System.out.println("----------------------------------------------------------------------------------");
                    System.out.println("Total items stored: " + dataMap.size());
                }
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private static void showBlockchainMenu(Peer peer) {
        System.out.println("\n=== Menu de Blockchain ===");
        System.out.println("1) View list of BLOCKCHAIN relationships.");
        System.out.println("2) Show the BLOCKCHAIN by prev hash.");
        System.out.println("3) Which size the blockchain has been viewed.");
        System.out.println("0) Back");

        System.out.print("choose an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        var engine = peer.getNetworkGateway().getBlockchainEngine();
        var organizer = engine.getBlockOrganizer();

        switch (choice) {
            case 1:
                System.out.println("List of blocks: ");
                if (organizer.getOrderedChain().isEmpty()) {
                    System.out.println("The blockchain is empty (just Genesis?).");
                } else {
                    for (Block b : organizer.getOrderedChain()) {
                        System.out.printf(b.toString());
                    }
                }
                break;
            case 2:
                System.out.print("Enter the previous hash: ");
                String prevHash = scanner.nextLine();

                System.out.println("Functionality not implemented.");
                Block b = organizer.getBlockByHash(prevHash);
                if (b == null){
                    System.out.println("The next moment does not exist in a block with this hash.");
                } else {
                    System.out.println("Search block: " + b.toString());
                }
                break;
            case 3:
                System.out.println("Size of blockchain: ");
                System.out.println("Total: " + organizer.getChainHeight());
                break;
            case 0:
                return;
            default:
                System.out.println("Option invalid!");
        }
    }

    private static void showAuctionMenu(Peer peer) {
        System.out.println("\n=== Auction Market ===");
        System.out.println("1) Create New Auction (Transaction)");
        System.out.println("2) Place Bid (Transaction)");
        System.out.println("3) List Active Auctions (Ledger)");
        System.out.println("4) Simulation: 2 Auctions + 40 Bids");
        System.out.println("5) Closed auction ledger");
        System.out.println("0) Back");

        System.out.print("Choose: ");

        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Returning to menu...");
            return;
        }

        switch (choice) {
            case 1:
                System.out.print("Item Description: ");
                String desc = scanner.nextLine();

                System.out.print("Starting Price: ");
                BigDecimal price;
                try {
                    price = new BigDecimal(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.err.println("[ERROR] Invalid price format! Must be a number. Aborting.");
                    break;
                }

                peer.getNetworkGateway().getAuctionEngine().createdLocalAuctions(price, peer);
                break;

            case 2:
                listActiveAuctions(peer);

                System.out.print("\nAuction ID (Hash): ");
                String auctionId = scanner.nextLine().trim();

                System.out.print("Valor do Lance: ");
                BigDecimal bidValue;
                try {
                    bidValue = new BigDecimal(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.err.println("[ERROR] Invalid bid format! Must be a number. Aborting.");
                    break;
                }

                peer.getNetworkGateway().getAuctionEngine().placeBidRequest(auctionId, bidValue, peer);
                break;

            case 3:
                listActiveAuctions(peer);
                break;
            case 4:
                runStressTest(peer);
                break;
            case 5:
                System.out.print("\nAuction ID (Hash): ");
                String auctionIdClosed = scanner.nextLine().trim();
                peer.getNetworkGateway().getAuctionEngine().closeAuctionRequest(auctionIdClosed, peer);
                break;
            case 0:
                return;
            default:
                System.out.println("Invalid.");
        }
    }

    private static void listActiveAuctions(Peer peer) {
        Map<String, AuctionState> auctionList = peer.getNetworkGateway().getAuctionEngine().getWorldState();

        System.out.println("\n=== AUCTION LIST (Ledger Confirmed) ===");

        if (auctionList.isEmpty()) {
            System.out.println(" >> No auctions registered on the Blockchain yet.");
            System.out.println(" >> (Note: Create an auction and wait for the block to be mined).");
            return;
        }

        for (AuctionState state : auctionList.values()) {
            System.out.println(state);
        }
    }

    private static void runStressTest(Peer peer) {
        new Thread(() -> {
            try {
                var auctionEngine = peer.getNetworkGateway().getAuctionEngine();
                System.out.println("\n[SIMULATION] === INICIANDO STRESS TEST ===");

                BigDecimal priceA = new BigDecimal("1000");
                String auctionIdA = auctionEngine.createdLocalAuctions(priceA, peer);
                waitForAuctionInLedger(peer, auctionIdA);

                BigDecimal priceB = new BigDecimal("500");
                String auctionIdB = auctionEngine.createdLocalAuctions(priceB, peer);
                waitForAuctionInLedger(peer, auctionIdB);

                System.out.println("[SIMULATION] 3. Firing 40 shots...");
                for (int i = 1; i <= 20; i++) {
                    BigDecimal bidA = priceA.add(BigDecimal.valueOf(i * 50));
                    auctionEngine.placeBidRequest(auctionIdA, bidA, peer);

                    BigDecimal bidB = priceB.add(BigDecimal.valueOf(i * 25));
                    auctionEngine.placeBidRequest(auctionIdB, bidB, peer);

                    Thread.sleep(100);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private static void waitForAuctionInLedger(Peer peer, String targetId) throws InterruptedException {
        var engine = peer.getNetworkGateway().getAuctionEngine();
        int attempts = 0;
        System.out.print("[SIMULATION] Awaiting mining of " + targetId.substring(0, 8) + "...");
        while (attempts < 30) {
            if (engine.getWorldState().containsKey(targetId)) {
                System.out.println(" Done!");
                return;
            }
            Thread.sleep(1000);
            System.out.print(".");
            attempts++;
        }
    }
}
