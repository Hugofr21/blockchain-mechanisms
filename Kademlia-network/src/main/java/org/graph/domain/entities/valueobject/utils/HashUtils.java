package org.graph.domain.entities.valueobject.utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class HashUtils {

    public static String calculateSha256(String input) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        }catch (NoSuchAlgorithmException e) {
            System.out.println("No such algorithm: " + e.getMessage());
        }
        return null;
    }

    public static BigInteger calculateHashFromNodeId(PublicKey publicKey, long nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = publicKey.getEncoded();
            ByteBuffer buffer = ByteBuffer.allocate(keyBytes.length + Long.BYTES);
            buffer.put(keyBytes);
            buffer.putLong(nonce);
            byte[] hash = digest.digest(buffer.array());
            return new BigInteger(1, hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static String toHexString(BigInteger value) {
        return value.toString(16);
    }

    public static BigInteger fromHexString(String hex) {
        return new BigInteger(hex, 16);
    }
}