package org.graph.adapter.network.message.network;

import java.io.Serializable;
import java.math.BigInteger;
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
        BigInteger id,
        int networkDifficulty,
        PublicKey publicKey,
        long timestamp,
        byte[] signature
) implements Serializable {}