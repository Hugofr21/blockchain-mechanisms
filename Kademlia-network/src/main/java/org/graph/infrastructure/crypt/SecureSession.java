package org.graph.infrastructure.crypt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.concurrent.atomic.AtomicLong;

public class SecureSession {
    private final SecretKeySpec aesKey;
    private final byte[] ivPrefix;
    private final AtomicLong nonceCounter;

    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public SecureSession(byte[] ecdhSharedSecret) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256", "BC");
        byte[] keyBytes = sha256.digest(ecdhSharedSecret);
        this.aesKey = new SecretKeySpec(keyBytes, "AES");

        this.ivPrefix = new byte[4];
        new SecureRandom().nextBytes(this.ivPrefix);

        this.nonceCounter = new AtomicLong(0);
    }

    /**
     * Encrypts the data and appends the IV used to the beginning of the resulting array.
     */
    public byte[] encrypt(byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

        byte[] iv = generateNextIV();
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] result = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, IV_LENGTH);
        System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);

        return result;
    }

    /**
     * Extracts the IV from the header and decrypts the packet, validating its authenticity (GCM tag).
     */
    public byte[] decrypt(byte[] encryptedPackage) throws Exception {
        if (encryptedPackage.length < IV_LENGTH) {
            throw new IllegalArgumentException("Cryptogram too short to contain the Initialization Vector..");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(encryptedPackage, 0, iv, 0, IV_LENGTH);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        int cipherTextLength = encryptedPackage.length - IV_LENGTH;
        byte[] ciphertext = new byte[cipherTextLength];
        System.arraycopy(encryptedPackage, IV_LENGTH, ciphertext, 0, cipherTextLength);

        return cipher.doFinal(ciphertext);
    }

    /**
     * Constructs the IV: [4 bytes Random Prefix] + [8 bytes Sequential Counter]
     */
    private byte[] generateNextIV() {
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(ivPrefix, 0, iv, 0, 4);

        long counter = nonceCounter.getAndIncrement();
        for (int i = 7; i >= 0; i--) {
            iv[4 + i] = (byte) (counter & 0xFF);
            counter >>= 8;
        }
        return iv;
    }
}