package org.graph.domain.entities.p2p;

import org.graph.domain.application.mechanism.ProofOfReputation;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Objects;

public class Node implements Serializable {
    private NodeId id;
    private String host;
    private int port;
    private transient ProofOfReputation myProofOfReputation;
    private final long nonce;
    private final int NETWORK_DIFFICULTY;


    public Node(String host, int port, PublicKey ownerPublicKey, long nonce, int networkDifficulty) {
        this.NETWORK_DIFFICULTY = networkDifficulty;
        this.id = NodeId.createFromProof(ownerPublicKey, nonce , NETWORK_DIFFICULTY);
        this.host = host;
        this.port = port;
        this.nonce = nonce;
        this.myProofOfReputation = new ProofOfReputation();
    }

    public int getPort() {
        return port;
    }
    public long getNonce() {return nonce;}
    public String getHost() {
        return host;
    }
    public NodeId getNodeId() {return id;}
    public ProofOfReputation getMyProofOfReputation() {return myProofOfReputation;}

    public int getNETWORK_DIFFICULTY() {
        return NETWORK_DIFFICULTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node that = (Node) o;
        return port == that.port && Objects.equals(id, that.id);
    }


    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
