package org.graph.adapter.provider;

import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.p2p.Node;

import java.math.BigInteger;
import java.util.List;

public interface IEventDispatcher {
    void sendMulticast(Message message, List<BigInteger> targetNodeIds);
    void sendUnicast(Message message, ConnectionHandler context);
    void broadcastExcept(Message message,  Node excludeN);
}
