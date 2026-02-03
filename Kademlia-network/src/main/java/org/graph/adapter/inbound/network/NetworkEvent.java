package org.graph.adapter.inbound.network;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.infrastructure.network.neigbour.NeighboursConnections;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.node.Node;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.Logger;

public class NetworkEvent implements IEventDispatcher {
    private final NeighboursConnections neighboursConnections;
    private final Logger logger;

    public NetworkEvent(NeighboursConnections neighboursConnections, Logger logger) {
        this.neighboursConnections = neighboursConnections;
        this.logger = logger;
    }

    @Override
    public void sendUnicast(Message message, ConnectionHandler context) {
        if (context == null || context.getOutputStream() == null) return;
        try {
            MessageUtils.sendMessage(context.getOutputStream(), message);
        } catch (Exception e) {
            logger.warning("[UNICAST] Failed to send to" + context.getRemoteNode().getHost());
        }
    }

    @Override
    public void sendMulticast(Message message, List<BigInteger> targetNodeIds) {
        for (BigInteger id : targetNodeIds) {
            ConnectionHandler handler = neighboursConnections.getNeighbourById(id);
            if (handler != null) {
                sendUnicast(message, handler);
            }
        }
    }

    @Override
    public void broadcastExcept(Message message, Node excludeNode) {
        BigInteger excludeId = (excludeNode != null) ? excludeNode.getNodeId().value() : null;
        for (ConnectionHandler handler : neighboursConnections.getActivesNeighbors()) {
            if (handler.getRemoteNode() != null) {
                BigInteger currentId = handler.getRemoteNode().getNodeId().value();
                if (excludeId == null || !currentId.equals(excludeId)) {
                    sendUnicast(message, handler);
                }
            }
        }
    }
}