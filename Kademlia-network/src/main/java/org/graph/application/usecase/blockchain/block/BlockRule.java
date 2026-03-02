package org.graph.application.usecase.blockchain.block;


import org.graph.domain.entities.block.Block;
import org.graph.application.usecase.blockchain.BlockchainUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockRule {
    private Map<String, Block> blockMap;
    private Map<String, List<Block>> orphanBlocks;
    private Map<Integer, Block> organizedChain;
    private BlockchainUseCase mBlockchain;

    public BlockRule(BlockchainUseCase blockchain) {
        this.mBlockchain = blockchain;
        this.blockMap = new ConcurrentHashMap<>();
        this.orphanBlocks = new ConcurrentHashMap<>();
        this.organizedChain = new ConcurrentHashMap<>();
    }

    public synchronized boolean addLocalBlock(Block block) {
        String currentHash = block.getCurrentBlockHash();

        if (blockMap.containsKey(currentHash)) return false;


        String prevHash = block.getHeader().getPreviousBlockHash();
        Block parent = blockMap.get(prevHash);

        if (parent == null && block.getNumberBlock() != 0) {
            System.err.println("[ERROR] Attempt to add orphaned local block" + block.getNumberBlock());
            return false;
        }

        if (validateAndAddToChain(block, parent)) {
            blockMap.put(currentHash, block);
            System.out.println("[DEBUG] Block " + block.getNumberBlock() + " added the chain");
            mBlockchain.getTransactionOrganizer().markTransactionsAsProcessed(block.getTransactions());
            mBlockchain.getTransactionOrganizer().cleanPool(block.getTransactions());

            processOrphans(currentHash);

            return true;
        }

        return false;
    }


    public synchronized boolean receiveBlock(Block block) {
        if (block == null) return false;

        String currentHash = block.getCurrentBlockHash();
        if (blockMap.containsKey(currentHash)) return false;

        String prevHash = block.getHeader().getPreviousBlockHash();
        Block parent = blockMap.get(prevHash);

        boolean isGenesis = (block.getNumberBlock() == 0);
        boolean hasParent = (parent != null);

        if (hasParent || isGenesis) {
            if (validateAndAddToChain(block, parent)) {
                blockMap.put(currentHash, block);
                processOrphans(currentHash);
                return true;
            }
        } else {
            System.out.println("[ORGANIZER] Buffering orphan block: " + block.getNumberBlock());
            orphanBlocks.computeIfAbsent(prevHash, k -> new ArrayList<>()).add(block);
            return true;
        }
        return false;
    }

    private boolean validateAndAddToChain(Block block, Block parent){
        if (!block.isValidBlock(parent)) {
            System.out.println("[INFO] Block invalid rejected!");
            return false;
        }

        organizedChain.put(block.getNumberBlock(), block);
        return true;
    }


    private void processOrphans(String parentHash) {
        List<Block> orphans = orphanBlocks.remove(parentHash);
        if (orphans != null) {
            System.out.println("[ORGANIZER] Found " + orphans.size() + " orphans waiting for parent " + parentHash);
            for (Block orphan : orphans) {
                Block parent = blockMap.get(parentHash);
                if (validateAndAddToChain(orphan, parent)) {
                    blockMap.put(orphan.getCurrentBlockHash(), orphan);
                    System.out.println("[ORGANIZER] Orphan " + orphan.getNumberBlock() + " connected to chain!");
                    processOrphans(orphan.getCurrentBlockHash());
                }
            }
        }
    }

    public List<Block> getOrderedChain() {
        List<Block> chain = new ArrayList<>();
        for (int i = 0; i <= getChainHeight(); i++) {
            Block block = organizedChain.get(i);
            if (block != null) {
                chain.add(block);
            }
        }
        return chain;
    }

    public int getChainHeight() {
        return organizedChain.isEmpty() ? -1 : Collections.max(organizedChain.keySet());
    }

    public int getOrphanCount() {
        return orphanBlocks.values().stream().mapToInt(List::size).sum();
    }

    public Block getBlockByNumber(int number) {
        return organizedChain.get(number);
    }

    public Block getLastBlock() {
        if (organizedChain.isEmpty()) return null;
        Integer maxId = Collections.max(organizedChain.keySet());
        return organizedChain.get(maxId);
    }

    public boolean contains(String hash) {
        for (Block block : organizedChain.values()) {
            if (block.getCurrentBlockHash().equals(hash)) return true;
        }
        return false;
    }

    public Block getBlockByHash(String hash) {
        for (Block block : organizedChain.values()) {
            if (block.getCurrentBlockHash().equals(hash)) return block;
        }
        return null;
    }

    public boolean isParentInChain(String parentHash) {
        return blockMap.containsKey(parentHash);
    }
}
