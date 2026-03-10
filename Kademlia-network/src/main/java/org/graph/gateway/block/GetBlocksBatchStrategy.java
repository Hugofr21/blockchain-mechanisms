package org.graph.gateway.block;

import org.graph.adapter.outbound.network.message.block.InventoryPayload;
import org.graph.domain.entities.message.Message;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.infrastructure.utils.SerializationUtils;

public class GetBlocksBatchStrategy {

    public void handle(Message message, ConnectionHandler handler) {
        try {
            Object payload = message.getPayload();
            if (payload instanceof byte[]) {
                payload = SerializationUtils.deserialize((byte[]) payload);
            }

            if (payload instanceof InventoryPayload invPayload) {
                handler.getPeer().getBlockEventManger()
                        .handleGetBlocksBatch(invPayload, handler);
            } else {
                System.err.println("[SYNC] Invalid payload type for GET_BLOCKS_BATCH");
            }
        } catch (Exception e) {
            System.err.println("[SYNC] Error processing GET_BLOCKS_BATCH: " + e.getMessage());
        }
    }
}
