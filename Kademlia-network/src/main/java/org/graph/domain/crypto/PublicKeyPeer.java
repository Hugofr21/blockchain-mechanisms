package org.graph.domain.crypto;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Base64;
import  java.security.PublicKey;

public class PublicKeyPeer implements Serializable {
    private static final long serialVersionUID = 1L;
    private PublicKey key;
    private BigInteger peerId;
    private final String fingerprint;

    public PublicKeyPeer(PublicKey key) {
        this.key = key;
        this.peerId = null;
        this.fingerprint = generateFingerprint(key);
    }

    public PublicKey getKey() {return key;}

    public String toBase64() {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public BigInteger getPeerId() { return peerId; }

    public String getFingerprint() { return fingerprint; }

    public void setPeerId(BigInteger peerId) {this.peerId = peerId;}

    private static String generateFingerprint(java.security.PublicKey key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getEncoded());
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("[ERROR] generating fingerprint.", e);
        }
    }
}
