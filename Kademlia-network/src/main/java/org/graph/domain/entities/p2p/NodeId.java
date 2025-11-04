package org.graph.domain.entities.p2p;

import org.graph.domain.utils.HashUtils;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Objects;

public class NodeId {
    private static final int MAX_LENGTH = 256;
    private static final int DIFICULTY = 2;
    private BigInteger nodeId;

    public NodeId(PublicKey ownerPublicKey){
        this.nodeId = generateIdWithPKI(ownerPublicKey);
    }

    public BigInteger getId() { return nodeId; }


    private BigInteger generateIdWithPKI(PublicKey ownerPublicKey) {
        return HashUtils.sha256(ownerPublicKey.getEncoded());
    }

    public BigInteger distanceBetween(NodeId node2) {
        if (node2 == null || node2.nodeId == null) {
            throw new IllegalArgumentException("[ERROR] NodeId invalid or null: " + node2);
        }
        return this.nodeId.xor(node2.nodeId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeId nodeId = (NodeId) o;
        return Objects.equals(nodeId, nodeId.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public String toString() {
        return nodeId.toString(16);
    }




}
