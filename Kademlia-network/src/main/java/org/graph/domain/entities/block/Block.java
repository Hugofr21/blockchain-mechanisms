package org.graph.domain.entities.block;

import org.graph.application.usecase.mining.MinerThreadBlock;
import org.graph.application.usecase.mining.MiningResultBlock;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.entities.tree.MerkleTree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Block implements Serializable {
    private final int numberBlock;
    private final BlockHeader header;
    private final List<Transaction> transactions;
    private String hashCache;
    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService minerPool = Executors.newFixedThreadPool(CORES);


    public Block(int version, int numberBlock, String hashPrev, List<Transaction> transactions, int difficulty) {
        this.numberBlock = numberBlock;
        this.transactions = transactions;
        MerkleTree tree = new MerkleTree(transactions);
        String root = tree.getRootHash();
        this.header = new BlockHeader(version, hashPrev, root, difficulty);
    }

    public String getCurrentBlockHash() { return hashCache; }
    public int getNumberBlock() { return numberBlock; }
    public BlockHeader getHeader() { return header; }
    public List<Transaction> getTransactions() { return transactions; }


    /**
     *
     * @param difficulty number prefix
     * @param numberThread number de task getting nonce
     * <p>
     */

    public void mineBlock(int difficulty, int numberThread) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean found = new AtomicBoolean(false);
        CompletionService<MiningResultBlock> completionService = new ExecutorCompletionService<>(minerPool);
        List<Future<MiningResultBlock>> futures = new ArrayList<>();

        long nonceRangePerThread = Long.MAX_VALUE / numberThread;

        for (int i = 0; i < numberThread; i++) {
            long startNonce = i * nonceRangePerThread;
            MinerThreadBlock miner = new MinerThreadBlock(
                    i, startNonce, nonceRangePerThread,
                    header.getPayloadForMining(), difficulty, found
            );
            futures.add(completionService.submit(miner));
        }

        MiningResultBlock result = null;

        try {
            for (int i = 0; i < numberThread; i++) {
                Future<MiningResultBlock> future = completionService.take();
                MiningResultBlock r = future.get();
                if (r != null) {
                    result = r;
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Mining execution encountered an anomaly: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            for (Future<MiningResultBlock> f : futures) {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            }
        }

        if (result != null) {
            applyMiningResult(result, startTime);
        } else {
            System.out.println("[MINER] Failed to mine block (No nonce found in range).");
        }
    }

    private void  applyMiningResult(MiningResultBlock result, long startTime){
        this.header.setNonce(result.nonce());
        this.hashCache = result.hash();
        long endTime = System.currentTimeMillis();

        System.out.println("\n[INFO] Block miner!");
        System.out.println("  Thread win: #" + result.threadId());
        System.out.println("  Nonce find: " + result.nonce());
        System.out.println("  Attempts: " + result.attempts());
        System.out.println("  Time: " + (endTime - startTime) + "ms");
        System.out.println("  Hash: " + hashCache.substring(0, 40) + "...");
        System.out.println("  Hash rate: " +
                (result.attempts() * 1000.0 / (endTime - startTime)) + " H/s");
    }

    /**
     * Valida se o bloco atual é válido em relação ao bloco pai (último bloco da cadeia).
     *
     * @param parent último bloco da blockchain (bloco anterior)
     * @return true se o bloco for válido relativamente ao bloco pai; false caso contrário
     *
     * Regras de validação:
     * 1. O campo previousHash deve corresponder ao hash do bloco pai.
     * 2. O número do bloco (blockNumber) deve ser sequencial em relação ao bloco pai.
     * 3. O timestamp do bloco deve ser posterior ao timestamp do bloco pai.
     * 4. O timestamp deve respeitar limites válidos para evitar ataques do tipo Time Warp.
     */

    public boolean isValidBlock(Block parent){
        // 1. Validação de Gênesis
        if (parent == null) {
            boolean isGenesis = this.numberBlock == 0;
            if(!isGenesis) System.out.println("[DEBUG] Rejected: Father is null and it is not Genesis.");
            return this.numberBlock == 0;
        }

        // 2. Encadeamento (Chain Link)
        if (!this.header.getPreviousBlockHash().equals(parent.getCurrentBlockHash())) {
            System.out.println("Invalid Previous Hash");
            return false;
        }

        // 3. Sequencialidade
        if (this.numberBlock != parent.getNumberBlock() + 1) {
            System.out.println("Invalid Sequence Number");
            return false;
        }

        // 4. Timestamp (Proteção contra Time Warp attack)
        // O bloco não pode ser mais velho que o pai, nem muito no futuro
        if (this.header.getTimestamp() <= parent.getHeader().getTimestamp()) {
            System.out.println("Timestamp too old");
            return false;
        }


        return true;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(header.getTimestamp()));

        sb.append("\n ===================================================================== \n");
        sb.append(String.format("| BLOCK #%-4d                                                       ║\n", numberBlock));
        sb.append("===================================================================== \n");

        sb.append(String.format("| Hash:      %s\n", (hashCache != null ? hashCache : "PENDING MINING")));
        sb.append(String.format("| Prev Hash: %s\n", header.getPreviousBlockHash()));
        sb.append(String.format("| Root:      %s\n", header.getMerkleRoot()));
        sb.append(String.format("| Nonce:     %-10d | Difficulty: %d\n", header.getNonce(), header.getDifficulty()));
        sb.append(String.format("| Time:      %s\n", timeStr));

        sb.append("===================================================================== \n");
        sb.append(String.format("| TRANSACTIONS (%d)                                             ║\n", transactions.size()));
        sb.append("===================================================================== \n");

        if (transactions.isEmpty()) {
            sb.append("| (Empty Block)                                                      ║\n");
        } else {
            for (Transaction tx : transactions) {
                sb.append("| > ").append(tx.toString()).append("\n");
            }
        }

        sb.append("===================================================================== ");
        return sb.toString();
    }




}
