package org.graph.adapter.network.kademlia;

import org.graph.adapter.network.message.node.FindNodePayload;
import org.graph.adapter.utils.Base64Utils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.p2p.Peer;
import org.graph.adapter.utils.MessageUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static org.graph.adapter.utils.Constants.NODE_K;

/*
  Um node quando quer entrar na rede conecta um bostrapp que seja autentificado para rede dele
  1. Autenticaçao dos nodes
  2. Envia Find Node para encontras  nodes para o seu id
  3. confrima a lista ao bostrapp

 */
public record JoinNetwork(Peer myPeer) {

    public void attemptJoin(String bootstrapHost, int bootstrapPort) {
        System.out.println("[JOIN] Conectando ao Bootstrap " + bootstrapHost + ":" + bootstrapPort);

        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(bootstrapHost, bootstrapPort), 5_000);

            ConnectionHandler handler = new ConnectionHandler(socket, myPeer, myPeer.getLogger());
            handler.initStreams();


            Optional<Node> optBootstrap = Handshake.doHandshake(
                    myPeer,
                    handler.getInputStream(),
                    handler.getOutputStream()
            );

            if (optBootstrap.isEmpty()) {
                System.err.println("[JOIN] Handshake rejeitado pelo Bootstrap.");
                socket.close();
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
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException ex) { }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void triggerBootstrapLookup(ConnectionHandler handler) {
        try {
            BigInteger targetId = myPeer.getMyself().getNodeId().value();
            FindNodePayload payload = new FindNodePayload(Base64Utils.encode(targetId.toByteArray()));
            Message lookup = new Message(MessageType.FIND_NODE, payload, myPeer.getHybridLogicalClock());
            MessageUtils.sendMessage(handler.getOutputStream(), lookup);

        } catch (IOException e) {
            System.err.println("[JOIN] Error of to send  Lookup: " + e.getMessage());
        }
    }
}