package org.graph.adapter.network.message.block;

import org.graph.domain.entities.block.Block;

import java.io.Serializable;

public record BlockPayload(Block block) implements Serializable {
}