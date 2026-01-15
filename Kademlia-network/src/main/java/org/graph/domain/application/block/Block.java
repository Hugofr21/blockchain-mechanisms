package org.graph.domain.application.block;

import org.graph.domain.application.mechanism.pow.MinerThreadBlock;
import org.graph.domain.application.mechanism.pow.MiningResult;
import org.graph.domain.application.mechanism.pow.MiningResultBlock;
import org.graph.domain.application.transaction.Transaction;
import org.graph.domain.application.tree.MerkleTree;

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

        List<Future<MiningResultBlock>> futures = new ArrayList<>();

        long nonceRangePerThread = Long.MAX_VALUE / numberThread;

        for (int i = 0; i < numberThread; i++) {
            long startNonce = i * nonceRangePerThread;
            MinerThreadBlock miner = new MinerThreadBlock(
                    i, (int) startNonce, (int) nonceRangePerThread,
                    header.getPayloadForMining(), difficulty, found
            );
            futures.add(minerPool.submit(miner));
        }

        MiningResultBlock result = null;

        try {
            for (Future<MiningResultBlock> future : futures) {
                try {

                    MiningResultBlock r = future.get();
                    if (r != null) {
                        result = r;
                        break;
                    }
                } catch (CancellationException e) {
                    System.out.println("Miner thread has been canceled");
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Mining interrupted: " + e.getMessage());
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
     *
     * @param parent LAST BLOCK
     * @return boolean
     *
     * Role valid:
     * // 1. The previousHash must point to the parent hash
     * // 2. The blockNumber must be sequential
     * // 3. The timestamp must be later than the parent's
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

        // 5. CRÍTICO: Validação do Proof of Work
        // Recalculamos o hash com o nonce declarado e verificamos a dificuldade
//        String recalculatedHash = this.header.calculateHash();
//        if (!recalculatedHash.equals(this.hashCache)) {
//            System.out.println("Hash integrity check failed");
//            return false;
//        }

//        if (!checkDifficulty(recalculatedHash, this.header.getDifficulty())) {
//            System.out.println("PoW Difficulty not met");
//            return false;
//        }

        return true;
    }



    private boolean checkDifficulty(String hash, int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0');
        return hash.startsWith(target);
    }

    @Override
    public String toString() {
        return "Block{" +
                "numberBlock=" + numberBlock +
                ", header=" + header +
                ", transactions=" + transactions +
                ", hashCache='" + hashCache + '\'' +
                '}';
    }




}
