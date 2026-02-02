package org.graph.adapter.network.kademlia;

import org.graph.adapter.network.Handshake;
import org.graph.adapter.network.message.node.FindNodePayload;
import org.graph.adapter.utils.Base64Utils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.server.Peer;
import org.graph.adapter.utils.MessageUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;

/**
 * When a node wants to join the network, it connects to a bootstrap node
 * that has been previously authenticated and authorized for that network.
 * The process occurs as follows:
 * the node authenticates with the network;
 * sends a FindNode message to locate nodes near its identifier;
 * validates and confirms the list of nodes received with the bootstrap node.
 */
public record JoinNetwork(Peer myPeer) {

    public void attemptJoin(String bootstrapHost, int bootstrapPort) {
        System.out.println("[JOIN] Connecting to Bootstrap " + bootstrapHost + ":" + bootstrapPort);

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
                System.err.println("[JOIN] Handshake rejected by Bootstrap.");
                socket.close();
                return;
            }

            Node bootstrapNode = optBootstrap.get();
            System.out.println("[JOIN] Bootstrap authenticated! ID: " + bootstrapNode.getNodeId());


            handler.setRemoteNode(bootstrapNode);
            myPeer.getNeighboursManager().addConnection(bootstrapNode, handler);
            myPeer.getRoutingTable().addNode(bootstrapNode);

            new Thread(handler).start();

           myPeer.getmChainSyncController().startInitialSync(handler);
            triggerBootstrapLookup(handler);


        } catch (IOException e) {
            System.err.println("[JOIN] Bootstrap offline or inaccessible: " + e.getMessage());
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException ex) { }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * After verifying the authenticity and reliability of the node,
     * a request is made to the network to locate the group
     * of nodes closest to the identifier of the node in question.
     *
     * @param handler active thread responsible for receiving all
     * responses from the network through the socket.
     */
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