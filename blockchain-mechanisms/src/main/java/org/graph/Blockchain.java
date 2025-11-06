package org.graph;

import org.graph.block.Block;
import org.graph.mechanisms.BlockOrganizer;
import org.graph.mechanisms.TransactionOrganizer;
import org.graph.transaction.Transaction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Blockchain {
    private final int numThreads;
    private final int currentDifficulty;
    private TransactionOrganizer mTransactionOrganizer;
    private BlockOrganizer mBlockOrganizer;
    private int maxTransactions;

    public Blockchain(int difficulty, int maxTx) {
        this.maxTransactions = maxTx;
        this.mTransactionOrganizer = new TransactionOrganizer(maxTransactions);
        this.mBlockOrganizer = new BlockOrganizer(this);
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.currentDifficulty = difficulty;
    }


    public TransactionOrganizer getTransactionOrganizer() {
        return mTransactionOrganizer;
    }

    public BlockOrganizer getBlockOrganizer() {
        return mBlockOrganizer;
    }


    public void createGenesisBlock() {
        List<Transaction> genesisTx = new ArrayList<>();
        genesisTx.add(new Transaction("System", "Genesis", 0));
        Block genesis = new Block(1 ,0, "0", genesisTx);
        genesis.mineBlock(currentDifficulty, numThreads);
        mBlockOrganizer.addLocalBlock(genesis);
    }

    public void addTransaction(Transaction tx) {
        mTransactionOrganizer.addTransaction(tx);

        if (mTransactionOrganizer.shouldCreateBlock()) {
            System.out.println("\n[BLOCKCHAIN] Limite de transações atingido! Criando bloco...");
            createNewBlock();
        }
    }

    public Block createNewBlock() {
        List<Transaction> transactions = mTransactionOrganizer.getTransactionsForBlock();

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("[INFO] Sem transações pendentes");
            return null;
        }

        int newBlockNumber = mBlockOrganizer.getChainHeight() + 1;
        String previousHash = "0";

        if (newBlockNumber > 0) {
            Block lastBlock = mBlockOrganizer.getBlockByNumber(newBlockNumber - 1);
            if (lastBlock != null) {
                previousHash = lastBlock.getCurrentBlockHash();
            }
        }

        Block newBlock = new Block(1, newBlockNumber , previousHash, transactions);
        newBlock.mineBlock(currentDifficulty, numThreads);

        mBlockOrganizer.addLocalBlock(newBlock);

        return newBlock;
    }


    public boolean isChainValid() {
        List<Block> chain = mBlockOrganizer.getOrderedChain();
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

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

    public void receiveBlockFromPeer(Block block) {
        mBlockOrganizer.receiveBlock(block);
    }


    public void printBlockchain() {
        List<Block> chain = mBlockOrganizer.getOrderedChain();
        System.out.println("\n========================================");
        System.out.println("         BLOCKCHAIN COMPLETA");
        System.out.println("========================================");
        for (Block block : chain) {
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

    public void printStatus() {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║         STATUS BLOCKCHAIN          ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("Altura da cadeia: " + mBlockOrganizer.getChainHeight());
        System.out.println("Blocos órfãos: " + mBlockOrganizer.getOrphanCount());
        System.out.println("Transações pendentes: " + mTransactionOrganizer.getPendingCount());
        System.out.println("Cadeia válida: " + isChainValid());
    }


}
