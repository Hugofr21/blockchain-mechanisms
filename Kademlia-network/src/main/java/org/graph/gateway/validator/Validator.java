package org.graph.gateway.validator;

import org.graph.domain.application.block.Block;
import org.graph.adapter.blockchain.block.BlockOrganizer;

import java.util.List;
public class Validator {

    public boolean validateBlockchain(Block block, int currentDifficulty) {
        if (block == null) {
            return false;
        }

        if (isPoWValid(block.getCurrentBlockHash(), currentDifficulty)) {
            System.err.println("[DEBUG] PoW invalid: " + block.getCurrentBlockHash());
            System.err.println("[DEBUG] Expected starts with: " + getTarget(currentDifficulty));
            return false;
        }

        return true;
    }


    private boolean isPoWValid(String currentBlockHash, int difficulty) {
        String target = getTarget(difficulty);
        return !currentBlockHash.startsWith(target);
    }

    private String getTarget(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }
}