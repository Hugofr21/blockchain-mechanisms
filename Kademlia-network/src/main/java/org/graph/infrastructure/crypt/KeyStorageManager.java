package org.graph.infrastructure.crypt;

import org.graph.domain.crypto.PublicKeyPeer;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Map;

public class KeyStorageManager  {
    Map<BigInteger, PublicKeyPeer> keyStorageNeighbour;


    public void deleteNeighborPublicKey(BigInteger peerId) {
    }

    public void saveNeighborPublicKey(BigInteger peerId, PublicKeyPeer neighborKey) {
    }
}
