package org.graph.adapter.provider;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.node.Node;

import java.math.BigInteger;
import java.util.List;

public interface IEventDispatcher {
    void sendMulticast(Message message, List<BigInteger> targetNodeIds);
    void sendUnicast(Message message, ConnectionHandler context);
    void broadcastExcept(Message message,  Node excludeN);
}
