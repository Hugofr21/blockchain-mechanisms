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
     * Executa o handshake usando streams que já foram abertos.
     * NÃO fecha os streams – eles permanecerão abertos para a comunicação
     * posterior.
     *
     * @return Optional<Node> contendo o Node remoto (já validado) ou empty se falhar.
     */
    public static Optional<Node> doHandshake(Peer myself,
                                             DataInputStream  in,
                                             DataOutputStream out) throws Exception {

        Logger logger = myself.getLogger();
        long ts = System.currentTimeMillis();
        String challenge = myself.getMyself().getNodeId().value().toString() + ":" + ts;
        byte[] signature = myself.getIsKeysInfrastructure().signMessage(challenge);

        HandshakePayload myPayload = new HandshakePayload(
                myself.getMyself().getHost(),
                myself.getMyself().getPort(),
                myself.getMyself().getNonce(),
                myself.getMyself().getNodeId().value(),
                myself.getMyself().getNETWORK_DIFFICULTY(),
                myself.getIsKeysInfrastructure().getOwnerPublicKey(),
                ts,
                signature
        );

        MessageUtils.sendMessage(out, new Message(MessageType.HELLO, myPayload, myself.getHybridLogicalClock().next()));
        Message response = MessageUtils.readMessage(in);


        if (response == null || response.getType() != MessageType.HELLO) {
            logger.warning("Invalid handshake response from " + out);
            return Optional.empty();
        }


        Object raw = response.getPayload();
        HandshakePayload remote;
        if (raw instanceof HandshakePayload hp) {
            remote = hp;
        } else if (raw instanceof byte[] bytes) {
            remote = (HandshakePayload) SerializationUtils.deserialize(bytes);
        } else {
            logger.severe("Unexpected handshake payload type: " + raw.getClass().getName());
            return Optional.empty();
        }


        Node newNode = new Node(remote.host(),remote.port(), remote.id(), remote.nonce(), remote.networkDifficulty());
        myself.getReputationsManager().getProofOfReputation(newNode.getNodeId().value());


        if (!validateRemoteIdentity(remote ,newNode)) {
            logger.severe("Handshake validation failed (PoW or signature).");
            return Optional.empty();
        }


        PublicKey pk = (PublicKey) remote.publicKey();
        myself.getIsKeysInfrastructure()
                .addNeighborPublicKey(newNode.getNodeId().value(), pk);

        return Optional.of(newNode);
    }


    private static boolean validateRemoteIdentity(HandshakePayload payload, Node remote) {
        try {
            PublicKey pk = payload.publicKey();
            if (pk == null) return false;

            boolean isIdValid = NodeId.isValidNode(remote, pk);

            if (!isIdValid) {
                System.err.println("[DEBUG]Handshake rejected: Invalid node ID or false PoW.");
                return false;
            }


            String challengeData = remote.getNodeId().value().toString() + ":" + payload.timestamp();

            boolean isSignatureValid = CryptoUtils.verifySignature(pk, challengeData, payload.signature());

            if (!isSignatureValid) {
                System.err.println("[DEBUG]Handshake rejected: Invalid signature.");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("[DEBUG] Identity validation error: " + e.getMessage());
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

                ConnectionHandler newHandler = new ConnectionHandler(socket, myself, myself.getLogger(), myself.getGlobalScheduler());
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