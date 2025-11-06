package org.graph;


import org.graph.block.Block;
import org.graph.transaction.Transaction;

import java.util.Arrays;
import java.util.List;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║  BLOCKCHAIN: LOCAL vs RECEIVED BLOCKS        ║");
        System.out.println("╚═══════════════════════════════════════════════╝\n");

        int difficulty = 3;
        int maxTxPerBlock = 3;

        // Cria duas blockchains (simulando dois peers)
        Blockchain blockchain1 = new Blockchain(difficulty, maxTxPerBlock);
        Blockchain blockchain2 = new Blockchain(difficulty, maxTxPerBlock);

        // Peer 1: Cria genesis
        blockchain1.createGenesisBlock();
        Block genesis = blockchain1.getBlockOrganizer().getBlockByNumber(0);

        // Peer 2: Recebe genesis
        blockchain2.receiveBlockFromPeer(genesis);

        System.out.println("\n=== PEER 1: ADICIONANDO TRANSAÇÕES (LOCAL) ===");
        blockchain1.addTransaction(new Transaction("Alice", "Bob", 10));
        blockchain1.addTransaction(new Transaction("Bob", "Carol", 5));
        blockchain1.addTransaction(new Transaction("Carol", "Dave", 3));
        // Auto-cria bloco 1 local

        Thread.sleep(1000);

        Block block1 = blockchain1.getBlockOrganizer().getBlockByNumber(1);

        System.out.println("\n=== PEER 2: RECEBE BLOCO 1 DE PEER 1 ===");
        blockchain2.receiveBlockFromPeer(block1);

        System.out.println("\n=== PEER 2: ADICIONANDO TRANSAÇÕES (LOCAL) ===");
        blockchain2.addTransaction(new Transaction("Dave", "Eve", 2));
        blockchain2.addTransaction(new Transaction("Eve", "Frank", 1));
        blockchain2.addTransaction(new Transaction("Frank", "Alice", 0.5));
        // Auto-cria bloco 2 local

        Thread.sleep(1000);

        Block block2 = blockchain2.getBlockOrganizer().getBlockByNumber(2);

        System.out.println("\n=== PEER 1: RECEBE BLOCO 2 DE PEER 2 ===");
        blockchain1.receiveBlockFromPeer(block2);

        // Teste de órfão
        System.out.println("\n=== TESTE: PEER 3 RECEBE BLOCOS FORA DE ORDEM ===");
        Blockchain blockchain3 = new Blockchain(difficulty, maxTxPerBlock);

        blockchain3.receiveBlockFromPeer(genesis);
        blockchain3.receiveBlockFromPeer(block2); // Órfão!
        blockchain3.receiveBlockFromPeer(block1); // Pai chega depois

        Thread.sleep(1000);

        // Status final
        System.out.println("\n\n╔═══════════════════════════════════════════════╗");
        System.out.println("║              STATUS FINAL                     ║");
        System.out.println("╚═══════════════════════════════════════════════╝");

        System.out.println("\n--- PEER 1 ---");
        blockchain1.printStatus();
        blockchain1.printBlockchain();

        System.out.println("\n--- PEER 2 ---");
        blockchain2.printStatus();
        blockchain2.printBlockchain();

        System.out.println("\n--- PEER 3 ---");
        blockchain3.printStatus();
        blockchain3.printBlockchain();

    }
}