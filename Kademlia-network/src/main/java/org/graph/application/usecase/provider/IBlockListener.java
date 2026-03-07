package org.graph.application.usecase.provider;

import org.graph.domain.entities.block.Block;

import java.util.List;

public interface IBlockListener {
    void onBlockCommitted(Block block);
    void onChainReorganized(List<Block> newChain);
}