package org.graph.domain.crypto;

import java.util.Base64;
import  java.security.PublicKey;

public class PublicKeyPeer {
    private PublicKey key;

    public PublicKeyPeer(PublicKey key) {
        this.key = key;
    }

    public PublicKey getKey() {
        return key;
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

}
