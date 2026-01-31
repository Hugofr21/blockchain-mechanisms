package org.graph.adapter.network.message.network;

import org.graph.adapter.networkTime.HybridLogicalClock;
import org.graph.domain.entities.p2p.Node;

import java.io.Serializable;
import java.security.PublicKey;

/**
 * <p>Send message init for node</p>
 * @param timestamp attack replay
 * @param publicKey send public key format encode
 */
public record HandshakePayload(
        String host,
        int port,
        long nonce,
        int networkDifficulty,
        PublicKey publicKey,
        long timestamp,
        byte[] signature
) implements Serializable {}