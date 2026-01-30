package org.graph.adapter.network.message.block.provider;

import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.domain.entities.message.Message;

public interface MessageStrategy {
    void handle(Message message, ConnectionHandler context);
}
