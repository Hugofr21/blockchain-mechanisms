package org.graph.gateway.validator;

import org.graph.domain.application.block.Block;
import org.graph.infrastructure.blockchain.block.BlockOrganizer;

import java.util.List;

public class Validator {
    public boolean isChainValid(BlockOrganizer mBlockOrganizer ,int currentDifficulty ) {
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

}
