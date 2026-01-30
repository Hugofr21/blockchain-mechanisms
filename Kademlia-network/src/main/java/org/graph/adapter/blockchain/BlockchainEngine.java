package org.graph.adapter.blockchain;

import org.graph.adapter.auction.AuctionEngine;
import org.graph.domain.application.block.Block;
import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.application.transaction.TransactionType;
import org.graph.domain.common.Pair;
import org.graph.adapter.blockchain.block.BlockOrganizer;
import org.graph.adapter.blockchain.block.TransactionOrganizer;

import java.util.ArrayList;
import java.util.List;

public class BlockchainEngine {
    private final int numThreads;
    private final int currentDifficulty;
    private final TransactionOrganizer mTransactionOrganizer;
    private final BlockOrganizer mBlockOrganizer;
    private final AuctionEngine auctionEngine;

    public BlockchainEngine(int difficulty, int maxTx) {
        this.mTransactionOrganizer = new TransactionOrganizer(maxTx);
        this.mBlockOrganizer = new BlockOrganizer(this);
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.currentDifficulty = difficulty;
        this.auctionEngine = new AuctionEngine();
    }


    public TransactionOrganizer getTransactionOrganizer() {
        return mTransactionOrganizer;
    }
    public AuctionEngine getAuctionEngine() {return auctionEngine;}
    public BlockOrganizer getBlockOrganizer() {
        return mBlockOrganizer;
    }


    public void createGenesisBlock() {
        if (mBlockOrganizer.getChainHeight() >= 0) {
            System.out.println("[INIT] Blockchain já inicializada. Genesis ignorado.");
            return;
        }

        List<Transaction> genesisTx = new ArrayList<>();
        genesisTx.add(new Transaction(TransactionType.REGULAR_TRANSFER));
        Block genesis = new Block(1, 0, "0", genesisTx, currentDifficulty);
        genesis.mineBlock(currentDifficulty, numThreads);

        mBlockOrganizer.addLocalBlock(genesis);
    }

    public void addTransaction(Transaction tx) {
        // TODO: verify Signature
        if (tx == null) {
            System.err.println("[SEC] Transação inválida rejeitada.");
            return;
        }

        mTransactionOrganizer.addTransaction(tx);

        if (mTransactionOrganizer.shouldCreateBlock()) {
            System.out.println("\n[MINER] Mempool cheio. Iniciando mineração...");
            createNewBlock();
        }
    }

    public void createNewBlock() {
        List<Transaction> transactions = mTransactionOrganizer.getTransactionsForBlock();

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("[INFO] Sem transações pendentes");
            return;
        }

        System.out.println("[MINER] Criando Bloco #" + getInfoBlock().key() + " apontando para " + getInfoBlock().value());

        Block newBlock = new Block(1,getInfoBlock().key() , getInfoBlock().value(), transactions, currentDifficulty);
        newBlock.mineBlock(currentDifficulty, numThreads);

        boolean added = mBlockOrganizer.addLocalBlock(newBlock);

        if (!added) {
            System.err.println("[MINER] Falha crítica: Bloco minerado foi rejeitado pelo organizador local!");
        }

    }

    private Block getBlock(List<Transaction> transactions) {

        Block newBlock = new Block(1, getInfoBlock().key() , getInfoBlock().value(), transactions, currentDifficulty);
        newBlock.mineBlock(currentDifficulty, numThreads);
        return newBlock;
    }

    public void receiveBlockFromPeer(Block block) throws InterruptedException {
        System.out.println("\n[BLOCKCHAIN] Recieving block from peer...");
        System.out.println("Current Block: " + block);
        Thread.sleep(100);
        mBlockOrganizer.receiveBlock(block);
    }

    private Pair<Integer, String> getInfoBlock (){
        Block parentBlock = mBlockOrganizer.getLastBlock();

        String previousHash;
        int newHeight;


        if (parentBlock != null) {
            previousHash = parentBlock.getCurrentBlockHash();
            newHeight = parentBlock.getNumberBlock() + 1;
        } else {
            previousHash = "0";
            newHeight = 0;
        }

        return new Pair<>(newHeight, previousHash);
    }




}
