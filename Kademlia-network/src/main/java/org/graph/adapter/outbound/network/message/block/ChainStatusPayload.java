package org.graph.adapter.outbound.network.message.block;

import java.io.Serializable;

public record ChainStatusPayload(
        String bestBlockHash,
        int blockHeight
) implements Serializable {}