package org.graph.gateway.block;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.domain.entities.message.Message;
import org.graph.gateway.provider.IMessageStrategy;

public class GetStatusStrategyI implements IMessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        context.getPeer().getmChainSyncController().handleStatusRequest(context);
    }
}