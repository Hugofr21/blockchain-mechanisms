package org.graph.adapter.provider;

import org.graph.domain.entities.block.Block;

public interface BlockListener {
    void onBlockCommitted(Block block);
}