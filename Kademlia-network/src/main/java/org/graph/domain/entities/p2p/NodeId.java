package org.graph.domain.entities.p2p;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Objects;


public record NodeId(BigInteger value) implements Serializable {
    public static final int ID_LENGTH_BITS = 256;

    public static NodeId createFromProof(PublicKey publicKey, long nonce, int difficulty) {
        BigInteger generatedId = calculateId(publicKey, nonce);

        // Validação Matemática: ID < 2^(256 - dificuldade)
        BigInteger target = BigInteger.ONE.shiftLeft(ID_LENGTH_BITS - difficulty);
        if (generatedId.compareTo(target) >= 0) {
            throw new SecurityException("Invalid PoW: NodeId does not meet difficulty target of " + difficulty);
        }

        return new NodeId(generatedId);
    }

    // Lógica central de cálculo do ID: SHA256(PublicKey + Nonce)
    private static BigInteger calculateId(PublicKey publicKey, long nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = publicKey.getEncoded();
            ByteBuffer buffer = ByteBuffer.allocate(keyBytes.length + Long.BYTES);
            buffer.put(keyBytes);
            buffer.putLong(nonce);
            byte[] hash = digest.digest(buffer.array());
            return new BigInteger(1, hash); // 1 garante que seja positivo
        } catch (Exception e) {
            throw new RuntimeException("Crypto failure", e);
        }
    }

    // Para fins de comparação de distância (XOR Metric do Kademlia)
    public BigInteger distanceBetweenNode(BigInteger other) {
        return this.value.xor(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeId nodeId = (NodeId) o;
        return Objects.equals(value, nodeId.value);
    }
}