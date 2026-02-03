package org.graph.domain.application.usecase.provider;

import org.graph.domain.entities.block.Block;

public interface BlockListener {
    void onBlockCommitted(Block block);
}