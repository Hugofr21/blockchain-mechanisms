package org.graph.block;

import org.graph.pow.MinerThread;
import org.graph.pow.MiningResult;
import org.graph.transaction.Transaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class Block {
    private String currentBlockHash;
    private long timestamp;
    private long nonce;
    private int numberBlock;
    private BlockHeader header;

    public Block(int version, int numberBlock, String hashPrev, List<Transaction> transactions) {
        this.numberBlock = numberBlock;
        this.header = new BlockHeader(version ,hashPrev, transactions);
        this.timestamp = Instant.now().toEpochMilli();
        this.nonce = 0;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public long getNonce() {
        return nonce;
    }
    public String getDataForMining() { return header.getVersion() + header.getPreviousBlockHash() + header.getMerkleRoot() + timestamp;}
    public String getCurrentBlockHash() { return currentBlockHash; }
    public int getNumberBlock() { return numberBlock; }
    public BlockHeader getHeader() { return header; }


    /**
     *
     * @param difficulty number prefix
     * @param numberThread number de task getting nonce
     * <p>
     */

    public void mineBlock(int difficulty , int numberThread){
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numberThread);
        AtomicBoolean found = new AtomicBoolean(false);

        List<Future<MiningResult>> futures = new ArrayList<>();
        int nonceRangePerThread = Integer.MAX_VALUE / numberThread;

        for (int i = 0; i < numberThread; i++) {
            int startNonce = i * nonceRangePerThread;
            MinerThread miner = new MinerThread(
                    i, startNonce, nonceRangePerThread,
                    getDataForMining(), difficulty, found
            );
            futures.add(executor.submit(miner));
        }

        MiningResult result = null;

        try {
            for (Future<MiningResult> future : futures) {
                MiningResult r = future.get();
                if (r != null) {
                    result = r;
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Mining thread interrupted: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }

        long endTime = System.currentTimeMillis();

        if (result != null) {
            this.nonce = result.getNonce();
            this.currentBlockHash = result.getHash();

            System.out.println("\n[INFO] Block miner!");
            System.out.println("  Thread win: #" + result.getThreadId());
            System.out.println("  Nonce find: " + result.getNonce());
            System.out.println("  Attempts: " + result.getAttempts());
            System.out.println("  Time: " + (endTime - startTime) + "ms");
            System.out.println("  Hash: " + currentBlockHash.substring(0, 40) + "...");
            System.out.println("  Hash rate: " +
                    (result.getAttempts() * 1000.0 / (endTime - startTime)) + " H/s");
        }

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
    private boolean isValidBlock(Block parent){
        if (parent == null) {
            return this.numberBlock == 0;
        }

        if (!this.header.getPreviousBlockHash().equals(parent.getCurrentBlockHash())) {
            return false;
        }

        if (this.numberBlock != parent.getNumberBlock() + 1) {
            return false;
        }


        if (this.timestamp < parent.getTimestamp()) {
            return false;
        }

        return true;
    }


}
