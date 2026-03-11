package org.graph.adapter.outbound.network.message.network;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PublicKey;

/**
 * Inicializa o envio de uma mensagem de handshake para um nó remoto.
 * Este método prepara os parâmetros necessários para estabelecer
 * comunicação segura entre dois nós, incluindo mecanismos de
 * proteção contra replay e material criptográfico para derivação
 * de segredo compartilhado.
 *
 * @param host endereço ou sub-rede do nó de destino
 * @param port porta de comunicação utilizada pelo nó remoto
 * @param nonce valor aleatório utilizado para prevenção de ataques de replay
 * @param publicKey chave pública do emissor codificada (formato encoded)
 * @param signature assinatura digital que autentica os dados enviados
 * @param timestamp marca temporal gerada pelo servidor para posterior
 *                  validação do desafio (challenge)
 * @param ephemeralPublicKey chave pública efêmera utilizada no processo
 *                           de derivação de segredo compartilhado através
 *                           do protocolo ECDH
 */
public record HandshakePayload(
        String host,
        int port,
        long nonce,
        BigInteger id,
        int networkDifficulty,
        PublicKey publicKey,
        long timestamp,
        byte[] signature,
        byte[] ephemeralPublicKey
) implements Serializable {}