package org.graph.gateway.block;

import org.graph.adapter.outbound.network.message.block.BlockPayload;
import org.graph.adapter.outbound.network.message.block.InventoryPayload;
import org.graph.adapter.outbound.network.message.block.InventoryType;
import org.graph.infrastructure.utils.SerializationUtils;
import org.graph.gateway.provider.IMessageStrategy;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;

public class GetBlockStrategyI implements IMessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        Object raw = message.getPayload();
        InventoryPayload payload = null;

        try {
            if (raw instanceof byte[] bytes) {
                payload = (InventoryPayload) SerializationUtils.deserialize(bytes);
            }
            else if (raw instanceof InventoryPayload p) {
                payload = p;
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to read payload in GetBlockStrategy: " + e.getMessage());
            return;
        }

        if (payload != null && payload.type() == InventoryType.BLOCK) {
            String hash = payload.hash();

            Block block = context.getPeer().getNetworkGateway()
                    .getBlockchainEngine().getBlockOrganizer()
                    .getBlockByHash(hash);

            if (block != null) {
                System.out.println("[DEBUG] Sending block " + block.getNumberBlock() + " to " + context.getRemoteNode().getPort());

                BlockPayload blockPayload = new BlockPayload(block);

                Message response = new Message(
                        MessageType.BLOCK,
                        blockPayload,
                        context.getPeer().getHybridLogicalClock()
                );

                context.getPeer().getNetworkEvent().sendUnicast(response, context);
            } else {
                 System.out.println("[DEBUG] Block not found: " + hash);
            }
        }
    }
}