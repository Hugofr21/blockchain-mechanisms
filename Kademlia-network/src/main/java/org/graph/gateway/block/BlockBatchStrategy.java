package org.graph.gateway.block;

import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.infrastructure.utils.SerializationUtils;

import java.util.List;

public class BlockBatchStrategy {

    @SuppressWarnings("unchecked")
    public void handle(Message message, ConnectionHandler handler) {
        try {
            Object payload = message.getPayload();

            if (payload instanceof byte[]) {
                payload = SerializationUtils.deserialize((byte[]) payload);
            }

            if (payload instanceof List<?>) {
                List<Block> batch = (List<Block>) payload;
                handler.getPeer().getmChainSyncController().handleBlockBatch(batch, handler);

            } else {
                System.err.println("[SYNC] Invalid payload type for BLOCK_BATCH. Expected List<Block>.");
            }
        } catch (Exception e) {
            System.err.println("[SYNC] Error processing BLOCK_BATCH: " + e.getMessage());
        }
    }
}