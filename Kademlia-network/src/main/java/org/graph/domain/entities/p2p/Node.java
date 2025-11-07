package org.graph.domain.entities.p2p;

import org.graph.domain.application.mechanism.ProofOfReputation;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Objects;

public class Node {
    private NodeId id;
    private String host;
    private int port;
    private ProofOfReputation myProofOfReputation;

    public Node(String host, int port, PublicKey ownerPublicKey) {
        this.id = new NodeId(ownerPublicKey);
        this.host = host;
        this.port = port;
        this.myProofOfReputation = new ProofOfReputation();
    }

    public int getPort() {
        return port;
    }
    public String getHost() {
        return host;
    }
    public NodeId getNodeId() {return id;}
    public ProofOfReputation getMyProofOfReputation() {return myProofOfReputation;}

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
