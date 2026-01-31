package org.graph.adapter.provider;

import org.graph.domain.application.block.Block;

public interface BlockListener {
    void onBlockCommitted(Block block);
}