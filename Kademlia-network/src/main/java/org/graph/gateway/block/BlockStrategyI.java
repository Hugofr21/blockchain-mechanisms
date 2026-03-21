package org.graph.gateway.block;


import org.graph.adapter.outbound.network.message.block.BlockPayload;
import org.graph.infrastructure.utils.SerializationUtils;
import org.graph.gateway.provider.IMessageStrategy;
import org.graph.infrastructure.network.ConnectionHandler;

import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;


public class BlockStrategyI implements IMessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        Object raw = message.getPayload();
        Block block = null;

        System.out.println("[BlockStrategy] Received payload type: " + (raw == null ? "null" : raw.getClass().getName()));

        try {
            if (raw instanceof byte[] bytes) {
                raw = SerializationUtils.deserialize(bytes);
                System.out.println("[BlockStrategy] Deserialized bytes for:" + raw.getClass().getName());
            }

            if (raw instanceof BlockPayload(Block block1)) {
                block = block1;
            } else if (raw instanceof Block b) {
                block = b;
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to deserialize block: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (block != null) {
            System.out.println("[BlockStrategy] Block extracted successfully: " + block.getCurrentBlockHash());
            context.getPeer().getBlockEventManger().handleBlock(block, context);
        } else {
            System.err.println("[ERROR] Null block after processing. Incorrect payload.");
        }
    }
}