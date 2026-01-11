package org.graph.domain.crypto;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PrivateKey;


public class KeyPairPeer implements Serializable {
    private static final long serialVersionUID = 1L;
    private PublicKeyPeer publicKey;
    private PrivateKey privateKey;
    private BigInteger peerId;

    public KeyPairPeer(PublicKeyPeer publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public void setPeerId(BigInteger peerId) {
        this.peerId = peerId;
        if (this.publicKey != null) {
            this.publicKey.setPeerId(peerId);
        }
    }

    public BigInteger getPeerId() { return peerId; }
    public PublicKeyPeer getPublicKey() { return publicKey; }
    public PrivateKey getPrivateKey() { return privateKey; }
    public String getFingerprint() {return publicKey != null ? publicKey.getFingerprint() : "null";}
}