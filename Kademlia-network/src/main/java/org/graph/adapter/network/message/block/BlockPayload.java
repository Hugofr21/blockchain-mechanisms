package org.graph.adapter.network.message.block;

import org.graph.domain.application.block.Block;

import java.io.Serializable;

public record BlockPayload(Block block) implements Serializable {
}