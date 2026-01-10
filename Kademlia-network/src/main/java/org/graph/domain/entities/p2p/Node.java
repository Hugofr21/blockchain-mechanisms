package org.graph.domain.entities.p2p;

import org.graph.domain.application.mechanism.ProofOfReputation;
import org.graph.domain.application.p2p.NeighboursConnections;
import org.graph.domain.application.pow.MiningResult;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Objects;

public class Node {
    private NodeId id;
    private String host;
    private int port;
    private ProofOfReputation myProofOfReputation;
    private final long nonce;
    private final static int NETWORK_DIFFICULTY = 2;


    public Node(String host, int port, PublicKey ownerPublicKey, MiningResult miningResult) {
        this.id = NodeId.createFromProof(ownerPublicKey, miningResult.getNonce(), NETWORK_DIFFICULTY);
        this.host = host;
        this.port = port;
        this.nonce = miningResult.getNonce();
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
