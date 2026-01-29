package org.graph.server.utils;

import org.graph.domain.entities.auctions.AuctionState;
import org.graph.infrastructure.p2p.Peer;

import java.math.BigDecimal;
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
            System.out.println("4) Created object auction and bidding.");
            System.out.println("5) Exit");

            System.out.print("Escolha uma opção: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    showMyPeerInfo(peer);
                    break;
                case 2:
                    showNeighboursMenu(peer);
                    break;
                case 3:
                    showBlockchainMenu(peer);
                    break;
                case 4:
                    showAuctionMenu(peer);
                    break;
                case 5:
                    System.out.println("Exist...");
                    return;
                default:
                    System.out.println("Option invalid!!");
            }
        }
    }



    private static void showMyPeerInfo(Peer peer) {
        System.out.println("\n=== Info of Peer ===");
        System.out.println("ID: " + peer.getMyself().getNodeId().value());
        System.out.println("Port: " + peer.getMyself().getPort());
        System.out.println("Current of Reputation: " + peer.getMyself().getMyProofOfReputation().getCurrentProofOfReputation());
    }

    private static void showNeighboursMenu(Peer peer) {
        System.out.println("\n=== Menu de Vizinhos ===");
        System.out.println("1) View list of NEIGHBOUR relationships.");
        System.out.println("2) Show the NEIGHBOUR by node id.");
        System.out.println("3) Which NEIGHBOUR with this peer have relationships high.");

        System.out.print("Choose a option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                System.out.println("List of NEIGHBOUR relationships: ");
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

        System.out.print("choose an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                System.out.println("List of blocks: ");
                break;
            case 2:
                System.out.print("Enter the previous hash: ");
                String prevHash = scanner.nextLine();

                System.out.println("Functionality not implemented.");
                break;
            case 3:
                System.out.println("Size of blockchain: ");
                break;
            default:
                System.out.println("Option invalid!");
        }
    }

    private static void showAuctionMenu(Peer peer) {
        System.out .println("\n=== Auction Market ===");
        System.out .println("1) Create New Auction");
        System.out .println("2) Bid");
        System.out .println("3) List Active Auctions");
        System.out .println("0) Back");

        System.out.print("Opção: ");
        int choice = Integer.parseInt(scanner.nextLine());

        switch (choice) {
            case 1:
                System.out .print("Item Description: ");
                String desc = scanner.nextLine ();

                System.out .print("Starting Price: ");
                BigDecimal price = new BigDecimal(scanner .nextLine());

                System.out .print("Duration (seconds): ");
                long duration = Long.parseLong(scanner .nextLine());

                peer.getAuctionEngine().createdLocalAuctions(price, peer.getMyself().getNodeId());
                break;

            case 2:

                listActiveAuctions(peer);

                System.out.print("\nID do Leilão (Hash): ");
                String auctionId = scanner.nextLine();

                System.out.print("Valor do Lance: ");
                BigDecimal bidValue = new BigDecimal(scanner.nextLine());

                peer.getAuctionEngine().placeBid(auctionId, bidValue, peer);
                break;

            case 3:
                listActiveAuctions(peer);
                break;

            case 0:
                return;
            default:
                System.out.println("Inválido.");
        }
    }

    private static void listActiveAuctions(Peer peer) {
        Map<String, AuctionState> auctionList = peer.getAuctionEngine().getWorldState();
        for (AuctionState A:auctionList.values()){
            System.out.println(auctionList.get(A.getAuctionId()));
        }
    }
}
