package org.graph.adapter.inbound.network;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.infrastructure.network.neighbor.NeighboursConnections;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.node.Node;
import java.math.BigInteger;
import java.net.SocketException;
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
        if (context == null) {
            logger.warning("[UNICAST] Aborted: Null ConnectionHandler.");
            return;
        }
        if (context.getOutputStream() == null) {
            logger.warning("[UNICAST] Aborted: OutputStream null for " + context.getRemoteNode().getHost());
            return;
        }

        try {
            System.out.println("[DEBUG_UNICAST]: SENDING UNICAST MESSAGE");
            MessageUtils.sendMessage(context.getOutputStream(), message);
        } catch (SocketException e) {
            logger.warning("[UNICAST] Broken tunnel to " + context.getRemoteNode().getHost() + ". Removing connection.");
            neighboursConnections.removeConnection(context.getRemoteNode().getNodeId().value());
        } catch (Exception e) {
            logger.severe("[UNICAST] Fatal error to be sent to " + context.getRemoteNode().getHost() + ": " + e.toString());
            e.printStackTrace();
            neighboursConnections.removeConnection(context.getRemoteNode().getNodeId().value());
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


    /**
     * Verifica no [ConnectionHandler] se o remoteNode não está null.
     *
     * Um socket deve estar aberto para permitir comunicação.
     * Em caso de falha normal, a mensagem pode ficar perdida.
     *
     * Não realizamos aqui verificações de conexões contra a estrutura
     * de broadcast, pois isso é tratado pelo mecanismo de heartbeat.
     **/
    @Override
    public void broadcastExcept(Message message, Node excludeNode) {
        BigInteger excludeId = (excludeNode != null) ? excludeNode.getNodeId().value() : null;
        for (ConnectionHandler handler : neighboursConnections.getActivesNeighbors()) {
                BigInteger currentId = handler.getRemoteNode().getNodeId().value();
                if (excludeId == null || !currentId.equals(excludeId)){
                    sendUnicast(message, handler);
                }

        }
    }
}