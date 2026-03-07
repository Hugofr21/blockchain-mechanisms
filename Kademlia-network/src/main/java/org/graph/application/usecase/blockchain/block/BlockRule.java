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
    private BlockchainUseCase mBlockchain;
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

    private synchronized boolean validateAndAddToChain(Block block, Block parent) {
        if (parent != null && !block.isValidBlock(parent)) {
            System.out.println("[INFO] Block invalid rejected!");
            return false;
        }

        // Fluxo Normal: Génese ou constrói diretamente em cima da nossa ponta atual
        if (currentTip == null || block.getHeader().getPreviousBlockHash().equals(currentTip.getCurrentBlockHash())) {
            organizedChain.put(block.getNumberBlock(), block);
            currentTip = block;
            return true;
        }

        // Fluxo de Fork: Bloco válido, mas aponta para outra ramificação. A cadeia dele é mais longa?
        if (block.getNumberBlock() > currentTip.getNumberBlock()) {
            System.out.println("[FORK DETECTADO] Cadeia concorrente é mais longa (" + block.getNumberBlock() + " vs " + currentTip.getNumberBlock() + "). Resolvendo...");
            executeChainReorganization(block);
            return true;
        } else {
            System.out.println("[FORK DETECTADO] Ramificação guardada, mas a nossa cadeia continua mais longa.");
            return false;
        }
    }

    private void executeChainReorganization(Block newTip) {
        List<Block> newBranch = new ArrayList<>();
        Block iterator = newTip;

        // 1. Caminha para trás na nova cadeia até cruzar com o ancestral comum
        while (iterator != null) {
            newBranch.add(iterator);
            Block oldBlockAtThisHeight = organizedChain.get(iterator.getNumberBlock());
            if (oldBlockAtThisHeight != null && oldBlockAtThisHeight.getCurrentBlockHash().equals(iterator.getCurrentBlockHash())) {
                break;
            }
            iterator = blockMap.get(iterator.getHeader().getPreviousBlockHash());
        }

        Collections.reverse(newBranch); // Coloca em ordem cronológica (Ancestral -> Nova Ponta)

        // 2. Extrair transações dos blocos que vão ser orfanados (descartados)
        List<Transaction> orphanedTxs = new ArrayList<>();
        int commonAncestorHeight = newBranch.getFirst().getNumberBlock();
        for (int i = commonAncestorHeight + 1; i <= currentTip.getNumberBlock(); i++) {
            Block deadBlock = organizedChain.get(i);
            if (deadBlock != null) {
                orphanedTxs.addAll(deadBlock.getTransactions());
            }
        }

        // 3. Atualizar a Cadeia Principal
        for (Block b : newBranch) {
            organizedChain.put(b.getNumberBlock(), b);
        }
        currentTip = newTip;

        // Limpar blocos fantasmas se a nova cadeia tiver menos altura temporariamente durante o cálculo
        for (int i = currentTip.getNumberBlock() + 1; i <= organizedChain.size(); i++) {
            organizedChain.remove(i);
        }

        // 4. Repor transações na Mempool e expurgar as que já estão na nova cadeia
        mBlockchain.getTransactionOrganizer().restoreToPool(orphanedTxs);
        for (Block b : newBranch) {
            mBlockchain.getTransactionOrganizer().cleanPool(b.getTransactions());
        }

        // NOVO: Anunciar ativamente aos vizinhos as transações ressuscitadas!
        if (!orphanedTxs.isEmpty()) {
            System.out.println("[REORG] Anunciando " + orphanedTxs.size() + " transações ressuscitadas à rede.");
            for (Transaction resurrectedTx : orphanedTxs) {
                // Utiliza a infraestrutura de rede para enviar a transação por Gossip
                org.graph.domain.entities.message.Message gossipMsg =
                        new org.graph.domain.entities.message.Message(
                                org.graph.domain.entities.message.MessageType.TRANSACTION,
                                resurrectedTx,
                                mBlockchain.getMyself().getHybridLogicalClock()
                        );

                // Dispara o broadcast para os vizinhos para que eles também a coloquem na Mempool
                mBlockchain.getMyself().getNetworkEvent().broadcastExcept(gossipMsg, null);
            }
        }

        // 5. Notificar a Máquina de Estados para fazer o Rollback (State Rebuild)
        List<Block> fullValidChain = getOrderedChain();
        mBlockchain.notifyChainReorganized(fullValidChain);

        System.out.println("[REORG CONCLUÍDO] Cadeia Principal assumiu ramificação vencedora.");
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
        return organizedChain.isEmpty() ? -1 : Collections.max(organizedChain.keySet()) + 1;
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
