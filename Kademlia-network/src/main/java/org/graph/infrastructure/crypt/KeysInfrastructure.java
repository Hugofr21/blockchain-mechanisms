package org.graph.infrastructure.crypt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.graph.domain.entities.valueobject.cryptography.KeyPairPeer;
import org.graph.domain.entities.valueobject.cryptography.PublicKeyPeer;
import org.graph.server.Peer;
import org.graph.adapter.utils.Base64Utils;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.graph.adapter.utils.Constants.ALGORITHM_CURVE;
import static org.graph.adapter.utils.Constants.ALGORITHM_INSTANCE;

public class KeysInfrastructure {
    private KeyPairPeer ownKeyPair;
    private final Map<String, PublicKeyPeer> neighborPublicKeys;
    private final Map<String, BigInteger> fingerprintToPeerId;
    private KeyStorageManager keyStorageManager;
    private Peer ownPeer;

    public KeysInfrastructure(Peer ownPeer, int port) {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.ownPeer = ownPeer;
        this.neighborPublicKeys = new ConcurrentHashMap<>();
        this.fingerprintToPeerId = new ConcurrentHashMap<>();
        this.keyStorageManager = new KeyStorageManager("peer_" + port);

        try {
            initializeKeys();
            if (this.ownKeyPair == null) {
                throw new IllegalStateException("FATAL: KeyPair is null after initialization.");
            }
        }catch (Exception e) {
            System.out.println("KeyStorageManager failed to initialize: " + e.getMessage());
        }
    }


    public PublicKey getOwnerPublicKey() {
        ensureKeysInitialized();
        return ownKeyPair.getPublicKey().getKey();
    }

    public KeyPairPeer getOwnerKeyPair() {
        ensureKeysInitialized();
        return ownKeyPair;
    }

    private void ensureKeysInitialized() {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Security Breach: Attempt to access keys before initialization.");
        }
    }

    private void initializeKeys() throws Exception {
        boolean loaded = false;

        // 1. Tenta carregar se o arquivo existir
        if (keyStorageManager.ownKeyPairExists()) {
            try {
                this.ownKeyPair = keyStorageManager.loadOwnKeyPair();
                if (this.ownKeyPair != null && this.ownKeyPair.getPublicKey() != null) {
                    System.out.println("[INFO] Keys loaded successfully. Fingerprint: " + ownKeyPair.getFingerprint());
                    loaded = true;
                }
            } catch (Exception e) {
                System.err.println("[WARN] Key file corrupted or incompatible. Generating new keys. Error: " + e.getMessage());
                // Se falhar carregar, não pare. Deixe cair no bloco de criação abaixo.
            }
        }

        // 2. Se não existia ou se falhou ao carregar, cria novas
        if (!loaded) {
            System.out.println("[INFO] Generating new KeyPair adapter...");
            createKeysInfrastructure();
            // Nota: Não salvamos imediatamente aqui, o Peer fará isso após minerar o ID.
            // Isso evita salvar chaves sem ID associado.
        }

        // 3. Carrega vizinhos (opcional, não deve impedir o boot se falhar)
        try {
            this.neighborPublicKeys.putAll(keyStorageManager.loadAllNeighborPublicKeys());
            neighborPublicKeys.forEach((fingerprint, key) -> {
                if (key.getPeerId() != null) {
                    fingerprintToPeerId.put(fingerprint, key.getPeerId());
                }
            });
        } catch (Exception e) {
            System.err.println("[WARN] Failed to load neighbors: " + e.getMessage());
        }
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
            System.out.println("[ERROR] creating keys in adapter: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("[ERROR] Invalid algorithm: " + e.getMessage());
        }
    }

    public void setOwnPeerIdAndSave(BigInteger peerId) throws Exception {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Not loading keys initiation!");
        }

        ownKeyPair.setPeerId(peerId);
        keyStorageManager.saveOwnKeyPair(ownKeyPair);

        System.out.println("Node save: " + peerId);
    }

    public void updateOwnPeerIdAndSave(BigInteger newPeerId) throws Exception {
        if (ownKeyPair == null) {
            throw new IllegalStateException("Keys not loaded yet!");
        }

        if (!ownKeyPair.getPeerId().equals(newPeerId)) {
            System.out.println("Node change to " + ownKeyPair.getPeerId() + " from " + newPeerId);
            ownKeyPair.setPeerId(newPeerId);
            keyStorageManager.saveOwnKeyPair(ownKeyPair);
        }
    }



    public void addNeighborPublicKey(BigInteger peerId, String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64Utils.decodeToBytes(publicKeyBase64);
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

        keyStorageManager.saveNeighborPublicKey(neighborKey);
        System.out.println("Vizinho adicionado - Fingerprint: " + fingerprint + ", PeerId: " + peerId);
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
        System.out.println("PeerId atualizado - Fingerprint: " + fingerprint + ", Novo PeerId: " + newPeerId);
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
        return ownKeyPair.getPublicKey().toBase64();
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
