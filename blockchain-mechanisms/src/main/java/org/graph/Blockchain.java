package org.graph;

import org.graph.block.Block;
import org.graph.transaction.Transaction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Blockchain {
    private LinkedList<Block> blockchain;
    private final int numThreads;
    private final int currentDifficulty;

    public Blockchain(int difficulty) {
        this.blockchain = new LinkedList<>();
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.currentDifficulty = difficulty;
    }


    public void createGenesisBlock() {
        List<Transaction> genesisTx = new ArrayList<>();
        genesisTx.add(new Transaction("System", "Genesis", 0));
        Block genesis = new Block(1 ,0, "0", genesisTx);
        genesis.mineBlock(currentDifficulty, numThreads);
        blockchain.add(genesis);
    }

    public void addBlock(List<Transaction> tx) {
        String prev = blockchain.getLast().getCurrentBlockHash();
        Block newBlock = new Block(1, blockchain.size() , prev, tx);
        newBlock.mineBlock(currentDifficulty, numThreads);
        blockchain.addLast(newBlock);
    }


    public boolean isChainValid() {
        for (int i = 1; i < blockchain.size(); i++) {
            Block current = blockchain.get(i);
            Block previous = blockchain.get(i - 1);

            if (!current.getHeader().getPreviousBlockHash().equals(previous.getCurrentBlockHash())) {
                return false;
            }

            String target = new String(new char[currentDifficulty]).replace('\0', '0');
            if (!current.getCurrentBlockHash().substring(0, currentDifficulty).equals(target)) {
                return false;
            }
        }
        return true;
    }


    public void printBlockchain() {
        System.out.println("\n========================================");
        System.out.println("         BLOCKCHAIN COMPLETA");
        System.out.println("========================================");
        for (Block block : blockchain) {
            System.out.println("\nBlock #" + block.getNumberBlock());
            System.out.println("Hash: " + block.getCurrentBlockHash());
            System.out.println("Hash Anterior: " + block.getHeader().getPreviousBlockHash());
            System.out.println("Merkle Root: " + block.getHeader().getMerkleRoot().substring(0, 32) + "...");
            System.out.println("Nonce: " + block.getNonce());
            System.out.println("Transaction Hash: ");
            for (Transaction tx : block.getHeader().getAllTransactions()) {
                System.out.println("  - " + tx);
            }
        }
        System.out.println("\n========================================");
    }

    public int getChainSize() { return blockchain.size(); }




}
