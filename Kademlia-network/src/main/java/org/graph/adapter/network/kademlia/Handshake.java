package org.graph.adapter.network.kademlia;

import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.utils.MessageUtils;
import org.graph.adapter.utils.SerializationUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.domain.entities.p2p.NodeId;
import org.graph.domain.utils.CryptoUtils;
import org.graph.adapter.network.message.network.HandshakePayload;
import org.graph.adapter.p2p.Peer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
     * @return Optional contendo o Node remoto (já validado) ou empty se falhar.
     */
    public static Optional<Node> doHandshake(Peer myPeer,
                                             DataInputStream  in,
                                             DataOutputStream out) throws Exception {

        Logger logger = myPeer.getLogger();
        long ts = System.currentTimeMillis();
        String challenge = myPeer.getMyself().getNodeId().value().toString() + ":" + ts;
        byte[] signature = myPeer.getIsKeysInfrastructure().signMessage(challenge);

        HandshakePayload myPayload = new HandshakePayload(
                myPeer.getMyself().getHost(),
                myPeer.getMyself().getPort(),
                myPeer.getMyself().getNonce(),
                myPeer.getMyself().getNETWORK_DIFFICULTY(),
                myPeer.getIsKeysInfrastructure().getOwnerPublicKey(),
                ts,
                signature
        );

        MessageUtils.sendMessage(out, new Message(MessageType.HELLO, myPayload, myPeer.getHybridLogicalClock().next()));
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


        Node newNode = new Node(remote.host(),remote.port(), remote.publicKey(), remote.nonce(), remote.networkDifficulty());
        System.out.println("[HANDSHAKE] Creadted new node: " + newNode);

        if (!validateRemoteIdentity(remote ,newNode)) {
            logger.severe("Handshake validation failed (PoW or signature).");
            return Optional.empty();
        }


        PublicKey pk = (PublicKey) remote.publicKey();
        myPeer.getIsKeysInfrastructure()
                .addNeighborPublicKey(newNode.getNodeId().value(), pk);

        return Optional.of(newNode);
    }


    private static boolean validateRemoteIdentity(HandshakePayload payload,Node remote) {
        try {
            PublicKey pk = (PublicKey) payload.publicKey();

            if (pk == null) return false;

            NodeId expected = NodeId.createFromProof(pk, remote.getNonce(), remote.getNETWORK_DIFFICULTY());
            if (!expected.equals(remote.getNodeId())) return false;
            String challenge = remote.getNodeId().value().toString() + ":" + payload.timestamp();
            return CryptoUtils.verifySignature(pk, challenge, payload.signature());

        } catch (Exception e) {
            return false;
        }
    }

    public static void connectAndVerify(String host, int port, Peer myPeer) {
        new Thread(() -> {
            try {

                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 3000);


                    ConnectionHandler newHandler = new ConnectionHandler(socket, myPeer, myPeer.getLogger());
                    newHandler.initStreams();

                    Optional<Node> handshakeResult = doHandshake(
                            myPeer,
                            newHandler.getInputStream(),
                            newHandler.getOutputStream()
                    );

                    if (handshakeResult.isPresent()) {
                        Node verifiedNode = handshakeResult.get();


                        newHandler.setRemoteNode(verifiedNode);

                        myPeer.getNeighboursManager().addConnection(verifiedNode, newHandler);

                        myPeer.getRoutingTable().addNode(verifiedNode);

                        new Thread(newHandler).start();

                        System.out.println("[TOPOLOGY] Node verified and added: " + verifiedNode.getNodeId());
                    } else {
                        System.err.println("[SECURITY] Handshake failed for " + host + ":" + port);
                    }
                }
            } catch (Exception e) {
                System.err.println("[NETWORK] Unreachable: " + host + ":" + port);
            }
        }).start();
    }

}