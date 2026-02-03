package org.graph.adapter.network.message.node;

import java.io.Serializable;

public record NodeInfoPayload(
        FindNodePayload nodeId,
        String host,
        int port
) implements Serializable { }
