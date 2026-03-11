package org.graph.adapter.inbound.network;

import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.node.Node;
import org.graph.server.Peer;

import java.util.List;
import java.util.Random;

/**
 * Garante a convergência eventual da Blockchain.
 * Se um bloco se perder durante a propagação Pub/Sub, este mecanismo
 * deteta a anomalia e engatilha a sincronização.
 */
public class AntiEntropyTask implements Runnable {
    private final Peer myself;
    private final Random random;

    public AntiEntropyTask(Peer myself) {
        this.myself = myself;
        this.random = new Random();
    }

    @Override
    public void run() {
        if (!myself.getIsRunning()) return;

        List<Node> activeNeighbors = myself.getNeighboursManager().getActiveNeighbours();
        if (activeNeighbors.isEmpty()) return;

        Node randomNeighbor = activeNeighbors.get(random.nextInt(activeNeighbors.size()));

        Message getStatusMsg = new Message(MessageType.GET_STATUS, null, myself.getHybridLogicalClock());

        myself.getLogger().info("[ANTI_ENTROPY] Checking synchronization with neighbor " + randomNeighbor.getPort());

        myself.getMkademliaNetwork().sendRPCAsync(randomNeighbor, getStatusMsg);
    }
}