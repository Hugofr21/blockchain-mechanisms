package org.graph.gateway.provider;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.domain.entities.message.Message;

public interface IMessageStrategy {
    void handle(Message message, ConnectionHandler context);
}
