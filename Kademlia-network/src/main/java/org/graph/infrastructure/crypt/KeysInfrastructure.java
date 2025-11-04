package org.graph.infrastructure.crypt;

import org.graph.domain.crypto.KeyPairPeer;
import org.graph.domain.crypto.PublicKeyPeer;
import org.graph.infrastructure.p2p.Peer;
import org.graph.infrastructure.utils.Base64Utils;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.graph.infrastructure.utils.Constants.ALGORITHM_CURVE;
import static org.graph.infrastructure.utils.Constants.ALGORITHM_INSTANCE;

public class KeysInfrastructure {
    private KeyPairPeer ownKeyPair;
    private final Map<String, PublicKeyPeer> neighborPublicKeys;
    private final Map<String, BigInteger> fingerprintToPeerId;
    private KeyStorageManager keyStorageManager;
    private Peer ownPeer;

    public KeysInfrastructure(Peer ownPeer) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        this.ownPeer = ownPeer;
        this.neighborPublicKeys = new ConcurrentHashMap<>();
        this.fingerprintToPeerId = new ConcurrentHashMap<>();
        this.keyStorageManager = new KeyStorageManager();

        try {
            initializeKeys();
        }catch (Exception e) {
            System.out.println("KeyStorageManager failed to initialize: " + e.getMessage());
        }
    }


    public PublicKey getOwnerPublicKey() { return ownKeyPair.getPublicKey(); }
    public KeyPairPeer getOwnerKeyPair() { return ownKeyPair;}


    private void initializeKeys() throws Exception {
        if (keyStorageManager.ownKeyPairExists()) {
            this.ownKeyPair = keyStorageManager.loadOwnKeyPair();
            System.out.println("[DEBUG] Loaded keys - Fingerprint: " + ownKeyPair.getFingerprint());
        } else {
            createKeysInfrastructure();
        }


        this.neighborPublicKeys.putAll(keyStorageManager.loadAllNeighborPublicKeys());
        neighborPublicKeys.forEach((fingerprint, key) ->
                fingerprintToPeerId.put(fingerprint, key.getPeerId())
        );
    }

    private void createKeysInfrastructure(){
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM_INSTANCE);
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(ALGORITHM_CURVE);
            keyGen.initialize(ecSpec, new SecureRandom());

            KeyPair keyPair = keyGen.generateKeyPair();
            PublicKeyPeer myPublic = new PublicKeyPeer(keyPair.getPublic());
            this.ownKeyPair = new KeyPairPeer(myPublic, keyPair.getPrivate());

        } catch (NoSuchAlgorithmException e) {
            System.out.println("[ERROR] creating keys in infrastructure: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("[ERROR] Invalid algorithm: " + e.getMessage());
        }
    }

    public void setOwnPeerIdAndSave(BigInteger peerId) throws Exception {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Not loading keys initiation!");
        }

        ownKeyPair.setPeerId(peerId);
        KeyStorageManager.saveOwnKeyPair(ownKeyPair);

        System.out.println("Node save: " + peerId);
    }

    public void updateOwnPeerIdAndSave(BigInteger newPeerId) throws Exception {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Keys not loaded yet!");
        }

        if (!ownKeyPair.getPeerId().equals(newPeerId)) {
            System.out.println("Node change to " + ownKeyPair.getPeerId() + " from " + newPeerId);
            ownKeyPair.setPeerId(newPeerId);
            KeyStorageManager.saveOwnKeyPair(ownKeyPair);
        }
    }



    public void addNeighborPublicKey(BigInteger peerId, String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64Utils.decodeString(publicKeyBase64);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_INSTANCE, "BC");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        java.security.PublicKey pubKey = keyFactory.generatePublic(keySpec);

        PublicKeyPeer neighborKey = new PublicKeyPeer(pubKey);
        neighborKey.setPeerId(peerId);
        String fingerprint = neighborKey.getFingerprint();

        neighborPublicKeys.put(fingerprint, neighborKey);
        fingerprintToPeerId.put(fingerprint, peerId);

        keyStorageManager.saveNeighborPublicKey(neighborKey);
        System.out.println("Neighbor added - Fingerprint: " + fingerprint + ", PeerId: " + peerId);
    }


    public void addNeighborPublicKey(BigInteger peerId, PublicKey publicKey) throws Exception {
        PublicKeyPeer neighborKey = new PublicKeyPeer(publicKey);
        neighborKey.setPeerId(peerId);
        String fingerprint = neighborKey.getFingerprint();

        neighborPublicKeys.put(fingerprint, neighborKey);
        fingerprintToPeerId.put(fingerprint, peerId);

        KeyStorageManager.saveNeighborPublicKey(neighborKey);
        System.out.println("✓ Vizinho adicionado - Fingerprint: " + fingerprint + ", PeerId: " + peerId);
    }

    public void updateNeighborPeerId(String fingerprint, BigInteger newPeerId) throws Exception {
        PublicKeyPeer neighborKey = neighborPublicKeys.get(fingerprint);
        if (neighborKey == null) {
            throw new IllegalArgumentException("Vizinho não encontrado: " + fingerprint);
        }


        PublicKeyPeer updatedKey = new PublicKeyPeer(neighborKey.getKey());
        neighborKey.setPeerId(newPeerId);
        neighborPublicKeys.put(fingerprint, updatedKey);
        fingerprintToPeerId.put(fingerprint, newPeerId);

        keyStorageManager.saveNeighborPublicKey(updatedKey);
        System.out.println("✓ PeerId atualizado - Fingerprint: " + fingerprint + ", Novo PeerId: " + newPeerId);
    }

    public void removeNeighborPublicKey(String fingerprint) throws Exception {
        neighborPublicKeys.remove(fingerprint);
        fingerprintToPeerId.remove(fingerprint);
        keyStorageManager.deleteNeighborPublicKey(fingerprint);
        System.out.println("✓ Vizinho removido - Fingerprint: " + fingerprint);
    }

    /**
     * Getting key the neighbour for the fingerprint
     */
    public PublicKeyPeer getNeighborByFingerprint(String fingerprint) {
        return neighborPublicKeys.get(fingerprint);
    }

    /**
     * Getting BigInteger peerId current of the neighbour fot he fingerprint
     */
    public BigInteger getPeerIdByFingerprint(String fingerprint) {
        return fingerprintToPeerId.get(fingerprint);
    }

    /**
     * Find fingerprint of the  neighbour for the BigInteger peerId
     */
    public String getFingerprintByPeerId(BigInteger peerId) {
        return fingerprintToPeerId.entrySet().stream()
                .filter(entry -> entry.getValue().equals(peerId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * List everything the neighbour
     */
    public void listNeighbors() {
        System.out.println("\n=== Neighbour Registrar  ===");
        if (neighborPublicKeys.isEmpty()) {
            System.out.println("None  neighbour registrar.");
        } else {
            neighborPublicKeys.forEach((fingerprint, key) -> {
                System.out.println("- Fingerprint: " + fingerprint + " | PeerId: " + key.getPeerId());
            });
        }
    }

    /**
     * Sign message
     */
    public byte[] signMessage(String message) throws Exception {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Par the keys not initialization.");
        }

        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initSign(ownKeyPair.getPrivateKey());
        signature.update(message.getBytes());

        return signature.sign();
    }

    /**
     * Verify signature using fingerprint
     */
    public boolean verifySignature(String fingerprint, String message, byte[] signatureBytes) throws Exception {
        PublicKeyPeer neighborKey = neighborPublicKeys.get(fingerprint);
        if (neighborKey == null) {
            throw new IllegalArgumentException("Neighbour not found: " + fingerprint);
        }

        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initVerify(neighborKey.getKey());
        signature.update(message.getBytes());

        return signature.verify(signatureBytes);
    }

    public String getOwnPublicKeyBase64() {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Par the keys not initialization.");
        }
        return ownKeyPair.getPublicKeyBase64();
    }

    public BigInteger getOwnPeerId() {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Par the keys not initialization.");
        }
        return ownKeyPair.getPeerId();
    }

    public String getOwnFingerprint() {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Par the keys not initialization.");
        }
        return ownKeyPair.getFingerprint();
    }

    public Map<String, PublicKeyPeer> getAllNeighbors() {
        return new ConcurrentHashMap<>(neighborPublicKeys);
    }


}
