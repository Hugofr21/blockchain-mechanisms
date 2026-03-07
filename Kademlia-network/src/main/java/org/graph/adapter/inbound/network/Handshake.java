package org.graph.adapter.inbound.network;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.adapter.utils.MessageUtils;
import org.graph.infrastructure.utils.SerializationUtils;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.domain.entities.node.NodeId;
import org.graph.adapter.utils.CryptoUtils;
import org.graph.adapter.outbound.network.message.network.HandshakePayload;
import org.graph.server.Peer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Optional;
import java.util.logging.Logger;


public final class Handshake {

    private Handshake() { /* no‑instantiation */ }

    /**
     * Executa o handshake de autenticação utilizando streams que já se encontram abertos.
     * <p>
     * Este method realiza um protocolo de autenticação mútua entre dois pares
     * sem fechar os streams fornecidos. Os streams permanecem abertos e podem
     * ser reutilizados para comunicação posterior após um handshake bem-sucedido.
     * </p>
     *
     * <p><b>Fluxo do handshake (Iniciador / Cliente TCP):</b></p>
     * <ol>
     *   <li>Envia a sua identidade pública para o nó remoto.</li>
     *   <li>Recebe a identidade do nó remoto juntamente com um desafio aleatório.</li>
     *   <li>Assina o desafio recebido utilizando a sua chave privada, provando
     *       a posse da chave privada associada à identidade pública.</li>
     *   <li>Envia o desafio assinado como prova de identidade (ACK).</li>
     *   <li>Aguarda a confirmação do nó remoto e obtém a assinatura remota,
     *       completando a autenticação mútua.</li>
     * </ol>
     *
     * <p>Se a autenticação for bem-sucedida, o nó remoto é validado e devolvido
     * como um {@code Node}. Caso contrário, é devolvido um {@link Optional#empty()}.</p>
     *
     * @return um {@link Optional} contendo o {@code Node} remoto validado
     *         caso o handshake seja bem-sucedido; caso contrário {@link Optional#empty()}.
     */

    public static Optional<Node> doHandshake(Peer myself, DataInputStream in, DataOutputStream out) throws Exception {
        Logger logger = myself.getLogger();


        HandshakePayload initPayload = new HandshakePayload(
                myself.getMyself().getHost(),
                myself.getMyself().getPort(),
                myself.getMyself().getNonce(),
                myself.getMyself().getNodeId().value(),
                myself.getMyself().getNETWORK_DIFFICULTY(),
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                System.currentTimeMillis(),
                null
        );
        MessageUtils.sendMessage(out, new Message(MessageType.HELLO, initPayload, myself.getHybridLogicalClock().next()));

        Message response = MessageUtils.readMessage(in);
        if (response == null || response.getType() != MessageType.HELLO) {
            logger.warning("Invalid handshake response structure.");
            return Optional.empty();
        }

        Object rawPayload = response.getPayload();
        HandshakePayload remotePayload;

        try {
            if (rawPayload instanceof HandshakePayload) {
                remotePayload = (HandshakePayload) rawPayload;
            } else if (rawPayload instanceof byte[]) {
                remotePayload = (HandshakePayload) SerializationUtils.deserialize((byte[]) rawPayload);
            } else {
                logger.severe("Unexpected handshake payload type.");
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.severe("Failed to deserialize HandshakePayload: " + e.getMessage());
            return Optional.empty();
        }

        Node remoteNode = new Node(remotePayload.host(), remotePayload.port(), remotePayload.id(), remotePayload.nonce(), remotePayload.networkDifficulty());

        String challengeToSign = remoteNode.getNodeId().value().toString() + ":" + remotePayload.timestamp();
        byte[] mySignature = myself.getIsKeysInfrastructure().signMessage(challengeToSign);

        Message ackMessage = new Message(MessageType.HELLO_ACK, mySignature, myself.getHybridLogicalClock().next());
        MessageUtils.sendMessage(out, ackMessage);

        Message finalConfirm = MessageUtils.readMessage(in);
        if (finalConfirm == null || finalConfirm.getType() != MessageType.HELLO_ACK) {
            logger.severe("Mutual authentication failed. Remote node dropped the connection.");
            return Optional.empty();
        }

        Object rawAckPayload = finalConfirm.getPayload();
        byte[] remoteSignature = null;

        try {
            if (rawAckPayload instanceof byte[]) {
                try {
                    Object deserialized = SerializationUtils.deserialize((byte[]) rawAckPayload);
                    if (deserialized instanceof byte[]) {
                        remoteSignature = (byte[]) deserialized;
                    } else if (deserialized instanceof HandshakePayload) {
                        remoteSignature = ((HandshakePayload) deserialized).signature();
                    } else {
                        remoteSignature = (byte[]) rawAckPayload;
                    }
                } catch (Exception e) {
                    remoteSignature = (byte[]) rawAckPayload;
                }
            } else if (rawAckPayload instanceof HandshakePayload) {
                remoteSignature = ((HandshakePayload) rawAckPayload).signature();
            } else {
                logger.severe("Invalid HELLO_ACK payload type.");
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.severe("Error parsing ACK payload: " + e.getMessage());
            return Optional.empty();
        }


        String remoteChallengeToVerify = myself.getMyself().getNodeId().value().toString() + ":" + initPayload.timestamp();

        if (!validateRemoteIdentity(remoteNode, remotePayload.publicKey(), remoteChallengeToVerify, remoteSignature)) {
            logger.severe("Mutual authentication validation failed. Connection aborted.");
            return Optional.empty();
        }


        myself.getReputationsManager().getProofOfReputation(remoteNode.getNodeId().value());
        myself.getIsKeysInfrastructure().addNeighborPublicKey(remoteNode.getNodeId().value(), remotePayload.publicKey());

        System.out.println("[SECURITY] Mutual Authentication successful with " + remoteNode.getHost() + ":" + remoteNode.getPort());
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

            if (!CryptoUtils.verifySignature(pk, challengeToVerify, remoteSignature)) {
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
                        newHandler.getInputStream(),
                        newHandler.getOutputStream()
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