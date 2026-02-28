package org.graph.adapter.outbound.network.message.node;

import java.io.Serializable;

public record NodeInfoPayload(
        FindNodePayload nodeId,
        String host,
        int port,
        long nonce,
        byte[] publicKey,
        int difficulty
) implements Serializable {}