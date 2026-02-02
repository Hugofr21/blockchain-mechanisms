package org.graph.gateway.block;

import org.graph.adapter.network.message.block.ChainStatusPayload;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.domain.entities.message.Message;
import org.graph.gateway.provider.MessageStrategy;

import org.graph.adapter.utils.SerializationUtils;

public class ChainStatusResponseStrategy implements MessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        Object raw = message.getPayload();
        ChainStatusPayload payload = null;

        if (raw instanceof ChainStatusPayload p) {
            payload = p;
        }

        else if (raw instanceof byte[] bytes) {
            try {
                Object obj = SerializationUtils.deserialize(bytes);
                if (obj instanceof ChainStatusPayload p) {
                    payload = p;
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to deserialize ChainStatusPayload: " + e.getMessage());
            }
        }

        if (payload != null) {
            System.out.println("[DEBUG] Valid payload! Hash: " + payload.bestBlockHash());
            context.getPeer().getmChainSyncController().handleStatusResponse(payload, context);
        } else {
            System.err.println("[ERROR] Payload ignored. ChainStatusPayload expected.");
        }
    }
}