package org.graph.server.utils;

import org.graph.domain.application.block.Block;
import org.graph.domain.entities.auctions.AuctionState;
import org.graph.server.Peer;
import org.graph.domain.entities.p2p.Node;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
            System.out.println("5) Exit");

            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1: showMyPeerInfo(peer); break;
                case 2: showNeighboursMenu(peer);break;
                case 3: showBlockchainMenu(peer);break;
                case 4: showAuctionMenu(peer);break;
                case 5: System.out.println("Exist..."); return;
                default: System.out.println("Option invalid!!");
            }
        }
    }



    private static void showMyPeerInfo(Peer peer) {
        System.out.println("\n=== Info of Peer ===");
        System.out.println("ID: " + peer.getMyself().getNodeId().value());
        System.out.println("Port: " + peer.getMyself().getPort());
//        System.out.println("Current of Reputation: " + peer.getMyself().getMyProofOfReputation().getCurrentProofOfReputation());
    }

    private static void showNeighboursMenu(Peer peer) {
        System.out.println("\n=== Kademlia DHT & Network Menu ===");
        System.out.println("1) List Active Connected Neighbours");
        System.out.println("2) DHT: Find Node (Lookup K-Closest)");
        System.out.println("3) DHT: Store Value (Publish Data)");
        System.out.println("4) DHT: Find Value (Search Block/Data)");
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
                String nodeId = scanner.nextLine();

                System.out.println("Functionality not implemented..");
                break;
            case 3:
                System.out.println("Neighbor with the most relationships:: ?");
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
        System.out.println("4) [MANUAL] Broadcast Last Block (Gossip/INV).");
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
            case 4:
                Block lastBlock = organizer.getLastBlock();
                if (lastBlock != null) {
                    System.out.println("[DEBUG] Starting the propagation of the Block#" + lastBlock.getNumberBlock());
                    peer.getNetworkGateway().announceBlockToNetwork(lastBlock);
                    System.out.println("[DEBUG] INV message sent to neighbors.");
                } else {
                    System.out.println("[DEBUG] There are no blocks to send (Empty chain?).");
                }
                break;
            case 0:
                return;
            default:
                System.out.println("Option invalid!");
        }
    }

    private static void showAuctionMenu(Peer peer) {
        System.out .println("\n=== Auction Market ===");
        System.out.println("1) Create New Auction (Transaction)");
        System.out.println("2) Place Bid (Transaction)");
        System.out.println("3) List Active Auctions (Ledger)");
        System.out.println("4) Simulation: 2 Auctions + 40 Bids");
        System.out.println("0) Back");

        System.out.print("Choose: ");
        int choice = Integer.parseInt(scanner.nextLine());

        switch (choice) {
            case 1:
                System.out .print("Item Description: ");
                String desc = scanner.nextLine ();

                System.out .print("Starting Price: ");
                BigDecimal price = new BigDecimal(scanner .nextLine());

//                System.out .print("Duration (seconds): ");
//                long duration = Long.parseLong(scanner .nextLine());

                peer.getNetworkGateway().getAuctionEngine().createdLocalAuctions(price, peer);
                break;

            case 2:

                listActiveAuctions(peer);

                System.out.print("\nAuction ID (Hash): ");
                String auctionId = scanner.nextLine();

                System.out.print("Valor do Lance: ");
                BigDecimal bidValue = new BigDecimal(scanner.nextLine());

                peer.getNetworkGateway().getAuctionEngine().placeBidRequest(auctionId, bidValue, peer);
                break;

            case 3:
                listActiveAuctions(peer);
                break;
            case 4:
                runStressTest(peer);
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
                auctionEngine.createdLocalAuctions(priceA, peer);


                String auctionIdA = waitForAuctionInLedger(peer, null);
                if (auctionIdA == null) return;

                BigDecimal priceB = new BigDecimal("500");
                auctionEngine.createdLocalAuctions(priceB, peer);

                String auctionIdB = waitForAuctionInLedger(peer, auctionIdA);
                if (auctionIdB == null) return;
                System.out.println("[SIMULATION] >> Auction B confirmed at Ledger:" + auctionIdB);


                System.out.println("[SIMULATION] 3. Firing 40 shots...");

                for (int i = 1; i <= 20; i++) {

                    BigDecimal bidA = priceA.add(BigDecimal.valueOf(1000 + i * 50));
                    auctionEngine.placeBidRequest(auctionIdA, bidA, peer);
                    System.out.println("[SIMULATION] Bid #" + i + "Sent to Car: " + bidA);


                    BigDecimal bidB = priceB.add(BigDecimal.valueOf(500 + i * 25));
                    auctionEngine.placeBidRequest(auctionIdB, bidB, peer);
                    System.out.println("[SIMULATION] Bid #" + i + "Sent to Painting:" + bidB);

                    Thread.sleep(100);
                }

                System.out.println("[SIMULATION] === This was sent successfully. ===");
                System.out.println("Please wait for the blocks to be mined to see the final result in the Menu 4 -> 3.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String waitForAuctionInLedger(Peer peer, String excludeId) throws InterruptedException {
        var engine = peer.getNetworkGateway().getAuctionEngine();
        int attempts = 0;

        System.out.print("[SIMULATION] Awaiting mining...");
        while (attempts < 30) {
            Map<String, AuctionState> ledger = engine.getWorldState();

            for (String id : ledger.keySet()) {

                if (!id.equals(excludeId)) {
                    System.out.println(" Done!");
                    return id;
                }
            }

            Thread.sleep(1000);
            System.out.print(".");
            attempts++;

        }

        System.err.println("\n[ERROR] Timeout! The auction was not mined in time. Check the difficulty or block size.");
        return null;
    }
}
