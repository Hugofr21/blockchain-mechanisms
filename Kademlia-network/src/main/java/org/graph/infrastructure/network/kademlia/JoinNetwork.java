package org.graph.infrastructure.network.kademlia;

import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.infrastructure.p2p.ConnectionHandler;
import org.graph.infrastructure.p2p.Peer;
import org.graph.infrastructure.utils.HandshakeUtils;
import org.graph.infrastructure.utils.MessageUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;


public record JoinNetwork(Peer myPeer) {

    public void attemptJoin(String bootstrapHost, int bootstrapPort) {
        System.out.println("[JOIN] Conectando ao Bootstrap " + bootstrapHost + ":" + bootstrapPort);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(bootstrapHost, bootstrapPort), 5_000);

            ConnectionHandler handler = new ConnectionHandler(socket, myPeer, myPeer.getLogger());
            handler.initStreams();

            Optional<Node> optBootstrap = HandshakeUtils.doHandshake(
                    myPeer,
                    handler.getInputStream(),
                    handler.getOutputStream()
            );
            if (optBootstrap.isEmpty()) {
                System.err.println("[JOIN] Handshake rejeitado pelo Bootstrap.");
                return;
            }
            Node bootstrapNode = optBootstrap.get();
            System.out.println("[JOIN] Bootstrap autenticado! ID: " + bootstrapNode.getNodeId());


            handler.setRemoteNode(bootstrapNode);
            myPeer.getNeighboursManager().addConnection(bootstrapNode, handler);
            myPeer.getRoutingTable().addNode(bootstrapNode);

            new Thread(handler).start();

            triggerBootstrapLookup(handler);

        } catch (IOException e) {
            System.err.println("[JOIN] Bootstrap offline ou inacessível: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void triggerBootstrapLookup(ConnectionHandler handler) {
        try {
            System.out.println("[JOIN] Disparando Lookup (FIND_NODE) buscando a mim mesmo...");

            BigInteger myId = myPeer.getMyself().getNodeId().value();
            Message lookup = new Message(MessageType.FIND_NODE, myId);

            // getOutputStream já está aberto (initStreams foi chamado antes)
            MessageUtils.sendMessage(handler.getOutputStream(), lookup);
        } catch (IOException e) {
            System.err.println("[JOIN] Erro ao enviar Lookup: " + e.getMessage());
        }
    }
}
