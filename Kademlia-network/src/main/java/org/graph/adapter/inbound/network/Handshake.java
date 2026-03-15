package org.graph.adapter.inbound.network;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.domain.entities.node.NodeId;
import org.graph.adapter.utils.CryptoUtils;
import org.graph.adapter.outbound.network.message.network.HandshakePayload;
import org.graph.server.Peer;
import org.graph.server.utils.MetricsLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Executa o protocolo de handshake de autenticação e estabelecimento de canal seguro
 * utilizando streams já abertos, sem os encerrar após a conclusão do processo.
 * <p>
 * Este método implementa um mecanismo de autenticação mútua entre dois pares,
 * permitindo simultaneamente a negociação de material criptográfico para a
 * proteção da comunicação subsequente. Os streams fornecidos permanecem ativos
 * após a conclusão do handshake, podendo ser reutilizados para a transmissão
 * segura de dados.
 * </p>
 *
 * <p><b>Sequência do protocolo (iniciador / cliente TCP):</b></p>
 * <ol>
 *   <li>Extração dos streams de comunicação diretamente a partir do {@code ConnectionHandler}.</li>
 *   <li>Geração de um par de chaves efémeras baseado em ECDH, utilizado para
 *       garantir Sigilo Perfeito Adiante (Perfect Forward Secrecy).</li>
 *   <li>Envio da mensagem {@code HELLO} em texto limpo, contendo a identidade
 *       pública e a chave efémera do iniciador.</li>
 *   <li>Receção da mensagem {@code HELLO} do nó remoto, também transmitida
 *       em texto limpo.</li>
 *   <li>Cálculo do segredo partilhado através do mecanismo ECDH com base
 *       nas chaves efémeras trocadas.</li>
 *   <li>Derivação da chave simétrica a partir do segredo partilhado e
 *       ativação do modo de cifragem no {@code ConnectionHandler}.</li>
 *   <li>A partir deste momento o canal passa a operar em modo protegido
 *       utilizando AES-GCM para confidencialidade e integridade.</li>
 *   <li>Envio da mensagem {@code HELLO_ACK}, já protegida pelo canal cifrado,
 *       comprovando a posse da chave privada associada à identidade pública.</li>
 *   <li>Receção da confirmação remota, igualmente cifrada, concluindo o
 *       processo de autenticação mútua.</li>
 * </ol>
 *
 * <p>
 * As mensagens {@code HELLO} são transmitidas em texto limpo, pois ocorrem
 * antes da negociação do segredo partilhado. A partir da mensagem
 * {@code HELLO_ACK}, todo o tráfego passa a ser protegido através de
 * cifragem autenticada AES-GCM, garantindo confidencialidade, integridade
 * e proteção contra modificação ou repetição de mensagens.
 * </p>
 *
 * <p>
 * Em caso de sucesso, o nó remoto autenticado é devolvido como uma instância
 * de {@code Node}. Caso a verificação criptográfica falhe ou o protocolo
 * não seja concluído corretamente, é devolvido {@link Optional#empty()}.
 * </p>
 *
 * @return um {@link Optional} contendo o {@code Node} remoto autenticado
 *         caso o handshake seja concluído com sucesso; caso contrário
 *         {@link Optional#empty()}.
 */

public final class Handshake {

    private Handshake() { /* no‑instantiation */ }

    public static Optional<Node> doHandshake(Peer myself, ConnectionHandler handler) throws Exception {
        Logger logger = myself.getLogger();

        long startTime = System.nanoTime();

        DataInputStream in = handler.getInputStream();
        DataOutputStream out = handler.getOutputStream();
        KeyPair ephemeralKeyPair = CryptoUtils.generateEphemeralKeyPair();

        HandshakePayload initPayload = new HandshakePayload(
                myself.getMyself().getHost(),
                myself.getMyself().getPort(),
                myself.getMyself().getNonce(),
                myself.getMyself().getNodeId().value(),
                myself.getMyself().getNETWORK_DIFFICULTY(),
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                System.currentTimeMillis(),
                null,
                ephemeralKeyPair.getPublic().getEncoded()
        );


        MessageUtils.sendMessage(out, new Message(MessageType.HELLO, initPayload, myself.getHybridLogicalClock().next()));


        Message response;
        try {
            response = MessageUtils.readMessage(in);
        } catch (IOException e) {
            logger.info("[HANDSHAKE] Remote peer closed connection before HELLO response. Host: " + handler.getSocket().getInetAddress());
            MetricsLogger.recordRpcError("HANDSHAKE_EOF_PREMATURE");
            MetricsLogger.recordOperationStatus("JOIN", "FAILED_NETWORK");

            return Optional.empty();
        }

        if (response == null || response.getType() != MessageType.HELLO) {
            logger.warning("Invalid handshake response structure.");
            return Optional.empty();
        }

        HandshakePayload remotePayload = MessageUtils.extractHandshakePayload(response, logger);
        if (remotePayload == null) return Optional.empty();



        Node remoteNode = new Node(remotePayload.host(), remotePayload.port(), remotePayload.id(), remotePayload.nonce(), remotePayload.networkDifficulty());

        long endTime = System.nanoTime();
        double handshakeLatencyMs = (endTime - startTime) / 1_000_000.0;
        MetricsLogger.recordLatency(remoteNode.getNodeId().value(), handshakeLatencyMs);

        byte[] sharedSecret = CryptoUtils.computeSharedSecret(ephemeralKeyPair.getPrivate(), remotePayload.ephemeralPublicKey());


        handler.enableSecureTransport(sharedSecret);


        String challengeToSign = remoteNode.getNodeId().value().toString() + ":" + remotePayload.timestamp();
        byte[] mySignature = myself.getIsKeysInfrastructure().signMessage(challengeToSign);

        Message ackMessage = new Message(MessageType.HELLO_ACK, mySignature, myself.getHybridLogicalClock().next());


        MessageUtils.sendSecureMessage(out, ackMessage, handler.getSecureSession());

        Message finalConfirm = MessageUtils.readSecureMessage(in, handler.getSecureSession());

        if (finalConfirm == null || finalConfirm.getType() != MessageType.HELLO_ACK) {

            logger.severe("Mutual authentication failed. Remote node dropped the connection.");
            return Optional.empty();
        }



        byte[] remoteSignature = MessageUtils.extractSignature(finalConfirm.getPayload(), logger);

        String remoteChallengeToVerify = myself.getMyself().getNodeId().value().toString() + ":" + initPayload.timestamp();

        if (!validateRemoteIdentity(remoteNode, remotePayload.publicKey(), remoteChallengeToVerify, remoteSignature)) {
            logger.severe("Mutual authentication validation failed. Connection aborted.");
            return Optional.empty();
        }

        myself.getReputationsManager().getProofOfReputation(remoteNode.getNodeId().value());
        myself.getIsKeysInfrastructure().addNeighborPublicKey(remoteNode.getNodeId().value(), remotePayload.publicKey());

        System.out.println("[SECURITY] Encrypted Tunnel (ECDHE + AES-GCM) established with " + remoteNode.getHost() + ":" + remoteNode.getPort());
        return Optional.of(remoteNode);
    }



    private static boolean validateRemoteIdentity(Node remote, PublicKey pk, String challengeToVerify, byte[] remoteSignature) {
        try {
            if (pk == null) {
                System.err.println("[SECURITY] Handshake rejected: Public key is null.");
                return false;
            }


            if (!NodeId.isValidNode(remote, pk)) {
                System.err.println("[SECURITY] Handshake rejected: Invalid node ID or false PoW (Sybil risk).");
                return false;
            }

            if (remoteSignature == null || remoteSignature.length == 0) {
                System.err.println("[SECURITY] Handshake rejected: Empty signature.");
                return false;
            }

            if (CryptoUtils.verifySignature(pk, challengeToVerify, remoteSignature)) {
                System.err.println("[SECURITY] Handshake rejected: Invalid signature. Forgery detected.");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("[SECURITY] Identity validation error: " + e.getMessage());
            return false;
        }
    }

    public static void connectAndVerify(String host, int port, Peer myself) {
        new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 3000);

                socket.setSoTimeout(5000);

                ConnectionHandler newHandler = new ConnectionHandler(socket, myself, myself.getLogger());
                newHandler.initStreams();

                Optional<Node> handshakeResult = doHandshake(
                        myself,
                        newHandler
                );

                if (handshakeResult.isPresent()) {
                    Node verifiedNode = handshakeResult.get();

                    newHandler.setRemoteNode(verifiedNode);

                    myself.getReputationsManager().reportEvent(
                            verifiedNode.getNodeId().value(),
                            EventTypePolicy.PING_SUCCESS
                    );

                    myself.getRoutingTable().addNode(verifiedNode, myself);

                    socket.setSoTimeout(0);

                    myself.getNeighboursManager().addConnection(verifiedNode, newHandler);

                    System.out.println("[TOPOLOGY] Node verified and added: " + verifiedNode.getNodeId());
                } else {
                    System.err.println("[SECURITY] Handshake failed for " + host + ":" + port);
                    socket.close();
                }
            } catch (Exception e) {
                System.err.println("[NETWORK] Unreachable / Error processing " + host + ":" + port + " - " + e.getMessage());
                if (socket != null && !socket.isClosed()) {
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }
        }).start();
    }

}