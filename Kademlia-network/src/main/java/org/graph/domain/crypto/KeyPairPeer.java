package org.graph.domain.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyPairPeer {
    private PublicKeyPeer publicKey;
    private final PrivateKey privateKey;

    public KeyPairPeer(PublicKeyPeer publicKey, PrivateKey privateKey, String peerId) {
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
}
