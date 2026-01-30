package org.graph.gateway.validator;

import org.graph.domain.application.block.Block;
import org.graph.adapter.blockchain.block.BlockOrganizer;

import java.util.List;

public class Validator {

    public boolean validateBlockchain(Block block, int currentDifficulty) {
        if (block == null) {
            return false;
        }

        if (isCheckDifficulty(block.getCurrentBlockHash(), currentDifficulty)) {
            System.err.println("[VALIDATEBLOCK] PoW invalid: " + block.getCurrentBlockHash());
            return false;
        }

        return true;
    }

    public boolean isChainValid(BlockOrganizer mBlockOrganizer ,int currentDifficulty ) {
        List<Block> chain = mBlockOrganizer.getOrderedChain();
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHeader().getPreviousBlockHash().equals(previous.getCurrentBlockHash())) {
                return false;
            }


            if (isCheckDifficulty(current.getCurrentBlockHash(), currentDifficulty)) {
                return false;
            }
        }

        return true;
    }

    private boolean isCheckDifficulty(String currentBlockHash, int currentDifficulty) {
        String target = new String(new char[currentDifficulty]).replace('\0', '0');
        return !currentBlockHash.equals(target);
    }

}
