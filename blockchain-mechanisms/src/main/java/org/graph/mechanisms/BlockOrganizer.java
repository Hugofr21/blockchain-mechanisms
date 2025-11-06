package org.graph.mechanisms;

import org.graph.Blockchain;
import org.graph.block.Block;

import java.util.*;
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

        System.out.println("\n[INFO] → Adicionando bloco local #" + block.getNumberBlock() +
                " (hash: " + currentHash.substring(0, 12) + "...)");

        if (blockMap.containsKey(currentHash)) {
            System.out.println("[LOCAL] Bloco já existe na cadeia");
            return false;
        }

        // Adiciona ao mapa
        blockMap.put(currentHash, block);

        // Para bloco local, sempre temos o pai na cadeia
        String prevHash = block.getHeader().getPreviousBlockHash();
        Block parent = blockMap.get(prevHash);

        if (validateAndAddToChain(block, parent)) {
            System.out.println("[LOCAL] ✓ Bloco #" + block.getNumberBlock() + " adicionado à cadeia");

            // Marca transações como processadas
            mBlockchain.getTransactionOrganizer().markTransactionsAsProcessed(block.getHeader().getAllTransactions());

            // Processa órfãos que podem depender deste bloco
            processOrphans(currentHash);
            return true;
        }

        return false;
    }

    public synchronized boolean receiveBlock(Block block) {
         String currentBlock = block.getCurrentBlockHash();

         if (blockMap.containsKey(currentBlock)) {
             System.out.println("Block " + currentBlock + " already exists");
             return false;
         }

        blockMap.put(currentBlock, block);

        String prevHash = block.getHeader().getPreviousBlockHash();
        Block parent = blockMap.get(prevHash);

        if (parent == null || block.getNumberBlock() == 0) {
            if (validateAndAddToChain(block, parent)) {
                System.out.println("Added block " + block.getNumberBlock() + " to chain");
                processOrphans(currentBlock);
            }
        }else {
            orphanBlocks.computeIfAbsent(prevHash, k -> new ArrayList<>()).add(block);
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
}
