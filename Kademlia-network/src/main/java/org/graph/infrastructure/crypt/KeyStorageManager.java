package org.graph.infrastructure.crypt;

import org.graph.domain.valueobject.cryptography.KeyPairPeer;
import org.graph.domain.valueobject.cryptography.PublicKeyPeer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

class KeyStorageManager {
    private final String rootDir;
    private final String ownKeysDir;
    private final String neighborKeysDir;
    private final char[] nodePassword;

    private static final String PRIVATE_KEY_FILE = "private.key";
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PEER_INFO_FILE = "peer.properties";

    private static final int ITERATIONS = 100000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;


    public KeyStorageManager(String peerIdentifier, char[] password) {
        this.rootDir = "storage_" + peerIdentifier + "/keys";
        this.ownKeysDir = this.rootDir + "/own";
        this.neighborKeysDir = this.rootDir + "/neighbors";
        this.nodePassword = password;
        initializeDirectories();
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(ownKeysDir));
            Files.createDirectories(Paths.get(neighborKeysDir));
        } catch (IOException e) {
            throw new RuntimeException("Error creating unique key directories for peer.", e);
        }
    }
    /**
     * Securely saves your own pair of keys.
     * (Removido 'static' para acessar 'ownKeysDir')
     */
    public void saveOwnKeyPair(KeyPairPeer keyPair) throws Exception {
        Path publicKeyPath = Paths.get(ownKeysDir, PUBLIC_KEY_FILE);
        byte[] publicBytes = keyPair.getPublicKey().getKey().getEncoded();
        Files.write(publicKeyPath, publicBytes);

        byte[] encryptedPrivateKey = encryptPrivateKey(keyPair.getPrivateKey().getEncoded());
        Path privateKeyPath = Paths.get(ownKeysDir, PRIVATE_KEY_FILE);
        Files.write(privateKeyPath, encryptedPrivateKey);

        Properties props = new Properties();
        props.setProperty("peerId", keyPair.getPeerId().toString());
        props.setProperty("fingerprint", keyPair.getFingerprint());
        props.setProperty("algorithm", "EC");
        props.setProperty("curve", "secp256k1");
        props.setProperty("createdAt", String.valueOf(System.currentTimeMillis()));

        Path propsPath = Paths.get(ownKeysDir, PEER_INFO_FILE);
        try (FileOutputStream fos = new FileOutputStream(propsPath.toFile())) {
            props.store(fos, "Peer Key Information");
        }

        System.out.println("[SECURITY] Saved keys with PBKDF2 encryption to " + ownKeysDir);
    }

    /**
     * Loading the pair keys owner
     */
    public KeyPairPeer loadOwnKeyPair() throws Exception {
        Path propsPath = Paths.get(ownKeysDir, PEER_INFO_FILE);

        if (!Files.exists(propsPath) || !Files.exists(Paths.get(ownKeysDir, PUBLIC_KEY_FILE))) {
            return null;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propsPath.toFile())) {
            props.load(fis);
        }
        BigInteger peerId = new BigInteger(props.getProperty("peerId"));
        String savedFingerprint = props.getProperty("fingerprint");

        Path publicKeyPath = Paths.get(ownKeysDir, PUBLIC_KEY_FILE);
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        Path privateKeyPath = Paths.get(ownKeysDir, PRIVATE_KEY_FILE);
        byte[] encryptedPrivateKey = Files.readAllBytes(privateKeyPath);

        byte[] privateKeyBytes = decryptPrivateKey(encryptedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        PublicKeyPeer publicKeyPeer = new PublicKeyPeer(publicKey);
        publicKeyPeer.setPeerId(peerId);
        KeyPairPeer keyPair = new KeyPairPeer(publicKeyPeer, privateKey);

        if (!keyPair.getFingerprint().equals(savedFingerprint)) {
            throw new SecurityException("Fingerprint integrity check failed.");
        }

        return keyPair;
    }

    /**
     * Saves a neighbor's public key using fingerprint as identifier.
     * (Removido 'static' para acessar 'neighborKeysDir')
     */
    public void saveNeighborPublicKey(PublicKeyPeer publicKey) throws Exception {
        Path neighborDir = Paths.get(neighborKeysDir, publicKey.getFingerprint());
        Files.createDirectories(neighborDir);

        Path publicKeyPath = neighborDir.resolve(PUBLIC_KEY_FILE);
        Files.write(publicKeyPath, publicKey.getKey().getEncoded());

        Properties props = new Properties();
        props.setProperty("peerId", publicKey.getPeerId().toString());
        props.setProperty("fingerprint", publicKey.getFingerprint());
        props.setProperty("addedAt", String.valueOf(System.currentTimeMillis()));

        Path propsPath = neighborDir.resolve(PEER_INFO_FILE);
        try (FileOutputStream fos = new FileOutputStream(propsPath.toFile())) {
            props.store(fos, "Neighbor Key Information");
        }
    }


    /**
     * Loads all public keys from neighbors
     */
    public Map<String, PublicKeyPeer> loadAllNeighborPublicKeys() throws Exception {
        Map<String, PublicKeyPeer> neighbors = new ConcurrentHashMap<>();
        Path neighborsDir = Paths.get(neighborKeysDir);

        if (!Files.exists(neighborsDir)) {
            return neighbors;
        }

        Files.list(neighborsDir)
                .filter(Files::isDirectory)
                .forEach(peerDir -> {
                    try {
                        Path propsPath = peerDir.resolve(PEER_INFO_FILE);
                        Properties props = new Properties();
                        try (FileInputStream fis = new FileInputStream(propsPath.toFile())) {
                            props.load(fis);
                        }
                        BigInteger peerId = new BigInteger(props.getProperty("peerId"));
                        String fingerprint = props.getProperty("fingerprint");

                        Path publicKeyPath = peerDir.resolve(PUBLIC_KEY_FILE);
                        byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
                        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
                        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                        PublicKey pubKey = keyFactory.generatePublic(keySpec);

                        PublicKeyPeer publicKey = new PublicKeyPeer(pubKey);
                        publicKey.setPeerId(peerId);

                        neighbors.put(fingerprint, publicKey);
                    } catch (Exception e) {
                        System.err.println("Error loading neighbor: " + e.getMessage());
                    }
                });
        return neighbors;
    }

    /**
     * Remove a neighbor's key using their fingerprint.
     * @String fingerprint
     */

    public void deleteNeighborPublicKey(String fingerprint) throws Exception {
        Path neighborDir = Paths.get(neighborKeysDir, fingerprint);
        if (Files.exists(neighborDir)) {
            Files.walk(neighborDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try { Files.delete(path); } catch (IOException e) { e.printStackTrace(); }
                    });
        }
    }

    public boolean ownKeyPairExists() {
        Path publicKeyPath = Paths.get(ownKeysDir, PUBLIC_KEY_FILE);
        Path privateKeyPath = Paths.get(ownKeysDir, PRIVATE_KEY_FILE);
        return Files.exists(publicKeyPath) && Files.exists(privateKeyPath);
    }

    private byte[] encryptPrivateKey(byte[] privateKeyBytes) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        SecretKey keySpec = deriveKey(salt);

        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
        byte[] encrypted = cipher.doFinal(privateKeyBytes);

        byte[] result = new byte[salt.length + iv.length + encrypted.length];
        System.arraycopy(salt, 0, result, 0, salt.length);
        System.arraycopy(iv, 0, result, salt.length, iv.length);
        System.arraycopy(encrypted, 0, result, salt.length + iv.length, encrypted.length);

        return result;
    }

    private byte[] decryptPrivateKey(byte[] fileData) throws Exception {
        if (fileData.length < SALT_LENGTH + IV_LENGTH) {
            throw new SecurityException("Encrypted key file is corrupted or too short.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        System.arraycopy(fileData, 0, salt, 0, SALT_LENGTH);

        SecretKey keySpec = deriveKey(salt);

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(fileData, SALT_LENGTH, iv, 0, IV_LENGTH);

        int cipherTextLength = fileData.length - SALT_LENGTH - IV_LENGTH;
        byte[] encrypted = new byte[cipherTextLength];
        System.arraycopy(fileData, SALT_LENGTH + IV_LENGTH, encrypted, 0, cipherTextLength);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

        return cipher.doFinal(encrypted);
    }

    private SecretKey deriveKey(byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(nodePassword, salt, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

}