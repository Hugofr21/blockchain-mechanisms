package org.graph.gateway.consensus;

import org.graph.domain.entities.block.Block;
import org.graph.gateway.provider.IConsensusEngine;

import java.math.BigInteger;

public class ProofOfWorkEngine implements IConsensusEngine {
    private final int targetDifficulty;
    private final int numThreads;

    public ProofOfWorkEngine(int targetDifficulty) {
        this.targetDifficulty = targetDifficulty;
        int availableCores = Runtime.getRuntime().availableProcessors();
        this.numThreads = Math.max(1, availableCores - 2);
    }

    @Override
    public void sealBlock(Block block) {
        block.mineBlock(targetDifficulty, numThreads);
    }

    @Override
    public boolean validateProof(Block block, Block parentBlock) {
        String hash = block.getCurrentBlockHash();
        String targetPrefix = new String(new char[targetDifficulty]).replace('\0', '0');
        return hash.startsWith(targetPrefix) && block.isValidBlock(parentBlock);
    }

    @Override
    public boolean isWinningChain(Block newTip, Block currentTip) {
        if (newTip.getNumberBlock() > currentTip.getNumberBlock()) {
            return true;
        } else if (newTip.getNumberBlock() == currentTip.getNumberBlock()) {
            BigInteger newHashVal = new BigInteger(newTip.getCurrentBlockHash(), 16);
            BigInteger currentHashVal = new BigInteger(currentTip.getCurrentBlockHash(), 16);
            return newHashVal.compareTo(currentHashVal) < 0;
        }
        return false;
    }

    @Override
    public int difficulty() {
        return targetDifficulty;
    }
}