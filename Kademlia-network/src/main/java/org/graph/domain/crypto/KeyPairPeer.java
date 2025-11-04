package org.graph.domain.crypto;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyPairPeer {
    private PublicKeyPeer publicKey;
    private final PrivateKey privateKey;

    public KeyPairPeer(PublicKeyPeer publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey.getKey();
    }

    public java.security.PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getPublicKeyBase64() {
        return publicKey.toBase64();
    }

    public BigInteger getPeerId() {
        return publicKey.getPeerId();
    }

    public String getFingerprint() {
        return publicKey.getFingerprint();
    }

    public void setPeerId(BigInteger peerId) {
        publicKey.setPeerId(peerId);
    }
}
