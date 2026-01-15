package org.graph.infrastructure.blockchain;

import org.graph.domain.application.block.Block;
import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.application.transaction.TransactionType;
import org.graph.infrastructure.blockchain.block.BlockOrganizer;
import org.graph.infrastructure.blockchain.block.TransactionOrganizer;

import java.util.ArrayList;
import java.util.List;

public class BlockchainEngine {
    private final int numThreads;
    private final int currentDifficulty;
    private TransactionOrganizer mTransactionOrganizer;
    private BlockOrganizer mBlockOrganizer;
    private int maxTransactions;

    public BlockchainEngine(int difficulty, int maxTx) {
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

        // CORREÇÃO CRÍTICA: Não use getChainHeight() + 1 cegamente.
        // Pegue o objeto do último bloco para garantir o hash correto.
        Block lastBlock = mBlockOrganizer.getLastBlock();

        String previousHash;
        int newBlockNumber;

        if (lastBlock != null) {
            previousHash = lastBlock.getCurrentBlockHash();
            newBlockNumber = lastBlock.getNumberBlock() + 1;
        } else {
            // Se não tem lastBlock, assume-se que é o Genesis (ou erro de inicialização)
            previousHash = "0";
            newBlockNumber = 0;
        }

        // Debug para garantir que estamos apontando para o pai certo
        System.out.println("[MINER] Criando Bloco #" + newBlockNumber + " apontando para " + previousHash);

        Block newBlock = new Block(1, newBlockNumber, previousHash, transactions, currentDifficulty);
        newBlock.mineBlock(currentDifficulty, numThreads);

        // Se falhar aqui, newBlock será válido estruturalmente, mas rejeitado logicamente
        boolean added = mBlockOrganizer.addLocalBlock(newBlock);

        if (!added) {
            System.err.println("[MINER] Falha crítica: Bloco minerado foi rejeitado pelo organizador local!");
        }

    }

    private Block getBlock(List<Transaction> transactions) {
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

        Block newBlock = new Block(1, newHeight , previousHash, transactions, currentDifficulty);
        newBlock.mineBlock(currentDifficulty, numThreads);
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

    public void receiveBlockFromPeer(Block block) throws InterruptedException {
        System.out.println("\n[BLOCKCHAIN] Recieving block from peer...");
        System.out.println("Current Block: " + block);
        Thread.sleep(100);
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
            System.out.println("Nonce: " + block.getHeader().getNonce());
            System.out.println("Transaction Hash: ");
            for (Transaction tx : block.getTransactions()) {
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
