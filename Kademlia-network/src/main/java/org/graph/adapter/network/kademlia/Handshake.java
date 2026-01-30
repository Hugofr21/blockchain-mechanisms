package org.graph.adapter.network.kademlia;

import org.graph.adapter.utils.MessageUtils;
import org.graph.adapter.utils.SerializationUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.domain.entities.p2p.NodeId;
import org.graph.domain.utils.CryptoUtils;
import org.graph.adapter.network.message.HandshakePayload;
import org.graph.adapter.p2p.Peer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
                myPeer.getMyself(),
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

        if (!validateRemoteIdentity(remote, myPeer.getMyself().getNETWORK_DIFFICULTY())) {
            logger.severe("Handshake validation failed (PoW or signature).");
            return Optional.empty();
        }

        Node remoteNode = remote.node();
        PublicKey pk = (PublicKey) remote.publicKey();
        myPeer.getIsKeysInfrastructure()
                .addNeighborPublicKey(remoteNode.getNodeId().value(), pk);

        return Optional.of(remoteNode);
    }


    private static boolean validateRemoteIdentity(HandshakePayload payload, int networkDifficulty) {
        try {
            Node node = payload.node();
            PublicKey pk = (PublicKey) payload.publicKey();

            if (pk == null) return false;

            NodeId expected = NodeId.createFromProof(pk, node.getNonce(), networkDifficulty);
            if (!expected.equals(node.getNodeId())) return false;

            String challenge = node.getNodeId().value().toString() + ":" + payload.timestamp();
            return CryptoUtils.verifySignature(pk, challenge, payload.signature());

        } catch (Exception e) {
            return false;
        }
    }
}