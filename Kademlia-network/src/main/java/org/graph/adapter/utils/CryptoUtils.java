package org.graph.adapter.utils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

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

    /**
     *
     * Reconstrói uma PublicKey a partir do array de bytes (formato X.509).
     * Essencial para reconstruir a identidade de nós recebidos via Kademlia.
     *
     */
    public static PublicKey getPublicKeyFromBytes(byte[] bytes) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory factory = KeyFactory.getInstance("EC", "BC");
            return factory.generatePublic(spec);
        } catch (Exception e) {
            System.err.println("[CRYPTO] Erro ao converter bytes para PublicKey: " + e.getMessage());
            // Retornar null ou lançar RuntimeException para não poluir a lógica com try-catch vazios
            return null;
        }
    }

}