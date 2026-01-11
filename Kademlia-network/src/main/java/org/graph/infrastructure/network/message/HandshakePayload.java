package org.graph.infrastructure.network.message;

import org.graph.domain.entities.p2p.Node;

import java.io.Serializable;

public record HandshakePayload(
        Node node,
        long timestamp,
        byte[] signature
) implements Serializable { }