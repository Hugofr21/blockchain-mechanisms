package org.graph.infrastructure.network.message;

import org.graph.domain.crypto.PublicKeyPeer;
import org.graph.domain.entities.p2p.Node;

import java.io.Serializable;
import java.security.PublicKey;

public record HandshakePayload(
        Node node,                  // Dados de endereço e ID
        PublicKey publicKey,    // A chave pública (que o Node não guardou)
        long timestamp,             // Para evitar Replay
        byte[] signature            // Assinatura de tudo isso
) implements Serializable {}