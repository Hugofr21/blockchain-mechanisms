package org.graph.application.usecase.provider;

import org.graph.domain.entities.block.Block;

public interface BlockListener {
    void onBlockCommitted(Block block);
}