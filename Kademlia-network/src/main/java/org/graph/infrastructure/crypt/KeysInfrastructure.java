package org.graph.infrastructure.crypt;

import org.graph.domain.crypto.PublicKeyPeer;
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
    private KeyPair ownKeyPair;
    private final Map<BigInteger, PublicKeyPeer> neighborPublicKeys;
    private KeyStorageManager keyStorageManager;

    public KeysInfrastructure(String password) {
        this.neighborPublicKeys = new ConcurrentHashMap<>();
        this.keyStorageManager = new KeyStorageManager();
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private void createKeysInfrastructure() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM_INSTANCE);
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(ALGORITHM_CURVE);
            keyGen.initialize(ecSpec, new SecureRandom());

            KeyPair keyPair = keyGen.generateKeyPair();
            this.ownKeyPair = new KeyPair(keyPair.getPublic(), keyPair.getPrivate());

        } catch (NoSuchAlgorithmException e) {
            System.out.println("[ERROR] creating keys in infrastructure: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("[ERROR] Invalid algorithm: " + e.getMessage());
        }
    }

    public void addNeighborPublicKey(BigInteger peerId, PublicKey publicKey) throws Exception {
        PublicKeyPeer neighborKey = new PublicKeyPeer(publicKey);
        neighborPublicKeys.put(peerId, neighborKey);
        keyStorageManager.saveNeighborPublicKey(peerId, neighborKey);
        System.out.println("✓ Chave pública adicionada e salva para vizinho: " + peerId);
    }

    public void addNeighborPublicKey(BigInteger nodeId, String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64Utils.decode(publicKeyBase64).getBytes();

        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_INSTANCE, "BC");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        java.security.PublicKey pubKey = keyFactory.generatePublic(keySpec);

        PublicKeyPeer neighborKey = new PublicKeyPeer(pubKey);
        neighborPublicKeys.put(nodeId, neighborKey);

        keyStorageManager.saveNeighborPublicKey(nodeId, neighborKey);
    }

    public void removeNeighborPublicKey(BigInteger nodeId) throws Exception {
        if (!neighborPublicKeys.containsKey(nodeId)) {
            System.out.println("[INFO] Remove key the neighbour not found");
            return;
        }
        neighborPublicKeys.remove(nodeId);
        keyStorageManager.deleteNeighborPublicKey(nodeId);
        System.out.println("[INFO] PK remove to neighbour: " + nodeId);
    }

    public PublicKey getNeighborPublicKey(BigInteger nodeId) {
        if (!neighborPublicKeys.containsKey(nodeId)) {
            System.out.println("[INFO] PK not found: " + nodeId);
            return null;
        }

        return neighborPublicKeys.get(nodeId).getKey();
    }


}
