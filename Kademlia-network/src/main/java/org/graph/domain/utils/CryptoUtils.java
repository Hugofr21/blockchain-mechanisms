package org.graph.domain.utils;

import java.security.PublicKey;
import java.security.Signature;

public class CryptoUtils {

    public static boolean verifySignature(PublicKey publicKey, String data, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
            signature.initVerify(publicKey);
            signature.update(data.getBytes());
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            System.err.println("[ERROR] In cryptographic verification.: " + e.getMessage());
            return false;
        }
    }
}