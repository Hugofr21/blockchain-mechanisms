package org.graph.application.usecase.blockchain.block;


import org.graph.domain.entities.block.Block;
import org.graph.application.usecase.blockchain.BlockchainUseCase;
import org.graph.domain.entities.transaction.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockRule {
    private Map<String, Block> blockMap;
    private Map<String, List<Block>> orphanBlocks;
    private Map<Integer, Block> organizedChain;
    private final BlockchainUseCase mBlockchain;
    private Block currentTip = null;

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
            System.err.println("[ERROR] Attempt to add orphaned local block: " + block.getNumberBlock());
            return false;
        }

        if (parent != null && !block.isValidBlock(parent)) {
            System.err.println("[INFO] Local block invalid rejected!");
            return false;
        }

        blockMap.put(currentHash, block);
        System.out.println("[DEBUG] Local Block " + block.getNumberBlock() + " saved to blockMap.");

        updateMainChainIfNecessary(block);

        mBlockchain.getTransactionOrganizer().markTransactionsAsProcessed(block.getTransactions());
        mBlockchain.getTransactionOrganizer().cleanPool(block.getTransactions());

        processOrphans(currentHash);

        return true;
    }

    public synchronized boolean receiveBlock(Block block) {
        if (block == null) return false;

        String currentHash = block.getCurrentBlockHash();
        if (blockMap.containsKey(currentHash)) return false;

        String prevHash = block.getHeader().getPreviousBlockHash();
        Block parent = blockMap.get(prevHash);

        boolean isGenesis = (block.getNumberBlock() == 0);

        if (!isGenesis && parent == null) {
            System.out.println("[ORGANIZER] Buffering orphan block: " + block.getNumberBlock());
            orphanBlocks.computeIfAbsent(prevHash, k -> new ArrayList<>()).add(block);
            return true;
        }

        if (parent != null && !block.isValidBlock(parent)) {
            System.out.println("[INFO] Remote block invalid rejected!");
            return false;
        }

        blockMap.put(currentHash, block);
        System.out.println("[DEBUG] Remote Block " + block.getNumberBlock() + " saved to blockMap.");

        updateMainChainIfNecessary(block);

        processOrphans(currentHash);

        return true;
    }

    private void updateMainChainIfNecessary(Block block) {
        if (currentTip == null || block.getHeader().getPreviousBlockHash().equals(currentTip.getCurrentBlockHash())) {
            organizedChain.put(block.getNumberBlock(), block);
            currentTip = block;
            System.out.println("[CHAIN] Extended main chain to height: " + block.getNumberBlock());
        }
        else if (block.getNumberBlock() > currentTip.getNumberBlock()) {
            System.out.println("[FORK DETECTED] Concurrent chain is longer (" + block.getNumberBlock() + " vs " + currentTip.getNumberBlock() + "). Resolving...");
            executeChainReorganization(block);
        } else {
            System.out.println("[FORK DETECTED] Branch stored in blockMap, but our main chain is still longer.");
        }
    }

    private void executeChainReorganization(Block newTip) {
        List<Block> newBranch = new ArrayList<>();
        Block iterator = newTip;

        while (iterator != null) {
            newBranch.add(iterator);
            Block oldBlockAtThisHeight = organizedChain.get(iterator.getNumberBlock());
            if (oldBlockAtThisHeight != null && oldBlockAtThisHeight.getCurrentBlockHash().equals(iterator.getCurrentBlockHash())) {
                break;
            }
            iterator = blockMap.get(iterator.getHeader().getPreviousBlockHash());
        }

        if (iterator == null) {
            System.err.println("[FATAL] Reorg failed: Common ancestor not found in local database.");
            return;
        }

        Collections.reverse(newBranch);

        List<Transaction> orphanedTxs = new ArrayList<>();
        int commonAncestorHeight = newBranch.getFirst().getNumberBlock();
        for (int i = commonAncestorHeight + 1; i <= currentTip.getNumberBlock(); i++) {
            Block deadBlock = organizedChain.get(i);
            if (deadBlock != null) {
                orphanedTxs.addAll(deadBlock.getTransactions());
            }
        }

        for (Block b : newBranch) {
            organizedChain.put(b.getNumberBlock(), b);
        }
        currentTip = newTip;

        for (int i = currentTip.getNumberBlock() + 1; i <= organizedChain.size(); i++) {
            organizedChain.remove(i);
        }

        mBlockchain.getTransactionOrganizer().restoreToPool(orphanedTxs);
        for (Block b : newBranch) {
            mBlockchain.getTransactionOrganizer().cleanPool(b.getTransactions());
        }

        if (!orphanedTxs.isEmpty()) {
            System.out.println("[REORG] Announcing " + orphanedTxs.size() + " transactions resurrected to the network.");
            for (Transaction resurrectedTx : orphanedTxs) {
                org.graph.domain.entities.message.Message gossipMsg =
                        new org.graph.domain.entities.message.Message(
                                org.graph.domain.entities.message.MessageType.TRANSACTION,
                                resurrectedTx,
                                mBlockchain.getMyself().getHybridLogicalClock()
                        );
                mBlockchain.getMyself().getNetworkEvent().broadcastExcept(gossipMsg, null);
            }
        }

        List<Block> fullValidChain = getOrderedChain();
        mBlockchain.notifyChainReorganized(fullValidChain);

        System.out.println("[REORG COMPLETED] Main Chain assumed winning branch.");
    }

    private void processOrphans(String parentHash) {
        List<Block> orphans = orphanBlocks.remove(parentHash);
        if (orphans != null) {
            System.out.println("[ORGANIZER] Found " + orphans.size() + " orphans waiting for parent " + parentHash);
            for (Block orphan : orphans) {
                Block parent = blockMap.get(parentHash);
                if (parent != null && orphan.isValidBlock(parent)) {
                    blockMap.put(orphan.getCurrentBlockHash(), orphan);
                    System.out.println("[ORGANIZER] Orphan " + orphan.getNumberBlock() + " connected to tree!");
                    updateMainChainIfNecessary(orphan);
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
        return organizedChain.isEmpty() ? -1 : Collections.max(organizedChain.keySet()) + 1;
    }

    public Block getLastBlock() {
        if (organizedChain.isEmpty()) return null;
        Integer maxId = Collections.max(organizedChain.keySet());
        return organizedChain.get(maxId);
    }

    public boolean contains(String hash) {
        if (hash == null) return false;
        return blockMap.containsKey(hash);
    }

    public Block getBlockByHash(String hash) {
        return blockMap.get(hash);
    }

    public boolean isParentInChain(String parentHash) {
        return blockMap.containsKey(parentHash);
    }
}