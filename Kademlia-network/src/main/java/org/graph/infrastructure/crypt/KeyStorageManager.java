package org.graph.infrastructure.crypt;

import org.graph.domain.valueobject.cryptography.KeyPairPeer;
import org.graph.domain.valueobject.cryptography.PublicKeyPeer;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
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

    private static final String PRIVATE_KEY_FILE = "private.key";
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PEER_INFO_FILE = "peer.properties";
    private static final String PASSWORD = "admin";


    public KeyStorageManager(String peerIdentifier) {
        this.rootDir = "storage_" + peerIdentifier + "/keys";
        this.ownKeysDir = this.rootDir + "/own";
        this.neighborKeysDir = this.rootDir + "/neighbors";

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
        // 1. Salvar Chave PÚBLICA (X.509)
        Path publicKeyPath = Paths.get(ownKeysDir, PUBLIC_KEY_FILE);
        byte[] publicBytes = keyPair.getPublicKey().getKey().getEncoded();
        Files.write(publicKeyPath, publicBytes);

        // 2. Salvar Chave PRIVADA (PKCS#8 Encrita)
        byte[] encryptedPrivateKey = encryptPrivateKey(keyPair.getPrivateKey().getEncoded());
        Path privateKeyPath = Paths.get(ownKeysDir, PRIVATE_KEY_FILE);
        Files.write(privateKeyPath, encryptedPrivateKey);

        // 3. Salvar Metadados
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

        System.out.println("Saved keys to " + ownKeysDir + " - Fingerprint: " + keyPair.getFingerprint());
    }

    /**
     * Loading the pair keys owner
     */
    public KeyPairPeer loadOwnKeyPair() throws Exception {
        Path propsPath = Paths.get(ownKeysDir, PEER_INFO_FILE);

        // Verifica existência baseada na pasta da instância
        if (!Files.exists(propsPath) || !Files.exists(Paths.get(ownKeysDir, PUBLIC_KEY_FILE))) {
            return null;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propsPath.toFile())) {
            props.load(fis);
        }
        BigInteger peerId = new BigInteger(props.getProperty("peerId"));
        String savedFingerprint = props.getProperty("fingerprint");

        // Carregar Pública
        Path publicKeyPath = Paths.get(ownKeysDir, PUBLIC_KEY_FILE);
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        // Carregar Privada
        Path privateKeyPath = Paths.get(ownKeysDir, PRIVATE_KEY_FILE);
        byte[] encryptedPrivateKey = Files.readAllBytes(privateKeyPath);
        byte[] privateKeyBytes = decryptPrivateKey(encryptedPrivateKey);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        PublicKeyPeer publicKeyPeer = new PublicKeyPeer(publicKey);
        publicKeyPeer.setPeerId(peerId);
        KeyPairPeer keyPair = new KeyPairPeer(publicKeyPeer, privateKey);

        if (!keyPair.getFingerprint().equals(savedFingerprint)) {
            throw new SecurityException("Fingerprint integrity check failed.");
        }

        System.out.println("Loaded keys from " + ownKeysDir);
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

    private static byte[] encryptPrivateKey(byte[] privateKeyBytes) throws Exception {
        SecretKeySpec keySpec = deriveKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
        byte[] encrypted = cipher.doFinal(privateKeyBytes);

        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return result;
    }

    private byte[] decryptPrivateKey(byte[] encryptedData) throws Exception {
        SecretKeySpec keySpec = deriveKey();

        byte[] iv = new byte[12];
        byte[] encrypted = new byte[encryptedData.length - 12];
        System.arraycopy(encryptedData, 0, iv, 0, 12);
        System.arraycopy(encryptedData, 12, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

        return cipher.doFinal(encrypted);
    }

    private static SecretKeySpec deriveKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(PASSWORD.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

}