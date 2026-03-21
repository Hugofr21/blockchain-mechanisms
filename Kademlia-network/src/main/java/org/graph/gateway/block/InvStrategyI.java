package org.graph.gateway.block;

import org.graph.adapter.outbound.network.message.block.InventoryPayload;
import org.graph.infrastructure.utils.SerializationUtils;
import org.graph.gateway.provider.IMessageStrategy;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.domain.entities.message.Message;



public class InvStrategyI implements IMessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        Object raw = message.getPayload();
        InventoryPayload payload = null;

        try {

            if (raw instanceof byte[] bytes) {
                payload = (InventoryPayload) SerializationUtils.deserialize(bytes);
            } else if (raw instanceof InventoryPayload p) {
                payload = p;
            }

            if (payload != null) {
                context.getPeer().getBlockEventManger().handleInv(payload, context);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to read inventory payload: " + e.getMessage());
        }
    }
}