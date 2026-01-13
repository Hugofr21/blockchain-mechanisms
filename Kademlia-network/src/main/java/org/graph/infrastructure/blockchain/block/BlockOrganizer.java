package org.graph.domain.application.mechanism.block;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockOrganizer {
    private Map<String, Block> blockMap;
    private Map<String, List<Block>> orphanBlocks;
    private Map<Integer, Block> organizedChain;
    private Blockchain mBlockchain;

    public BlockOrganizer(Blockchain blockchain) {
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
            System.err.println("[ERRO CRÍTICO] Tentativa de adicionar bloco local órfão #" + block.getNumberBlock());
            return false;
        }

        if (validateAndAddToChain(block, parent)) {
            blockMap.put(currentHash, block);
            System.out.println("[LOCAL] ✓ Block #" + block.getNumberBlock() + " added the chain");
            mBlockchain.getTransactionOrganizer().markTransactionsAsProcessed(block.getTransactions());
            mBlockchain.getTransactionOrganizer().cleanPool(block.getTransactions());

            processOrphans(currentHash);

            return true;
        }

        return false;
    }


    public synchronized void receiveBlock(Block block) {
        if (block == null) return;

        String currentHash = block.getCurrentBlockHash();
        if (blockMap.containsKey(currentHash)) return;

        String prevHash = block.getHeader().getPreviousBlockHash();
        Block parent = blockMap.get(prevHash);

        boolean isGenesis = (block.getNumberBlock() == 0);
        boolean hasParent = (parent != null);

        if (hasParent || isGenesis) {
            if (validateAndAddToChain(block, parent)) {
                blockMap.put(currentHash, block);
                processOrphans(currentHash);
            }
        } else {
            orphanBlocks.computeIfAbsent(prevHash, k -> new ArrayList<>()).add(block);
        }

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
            for (Block orphan : orphans) {
                Block parent = blockMap.get(parentHash);
                if (validateAndAddToChain(orphan, parent)) {
                    System.out.println("[INFO] Orphan #" + orphan.getNumberBlock() + " dotard!");
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
}
