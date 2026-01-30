package org.graph.adapter.network.message.block.data;

import org.graph.adapter.blockchain.BlockchainEngine;
import org.graph.adapter.network.message.block.InventoryPayload;
import org.graph.adapter.network.message.block.provider.MessageStrategy;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;

public class GetDataStrategy implements MessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        InventoryPayload payload = (InventoryPayload) message.getPayload();
        BlockchainEngine engine = context.getPeer().getNetworkGateway().getBlockchainEngine();

        if (payload.getTypeInventoryBlock()) {
            String hash = payload.getInventoryHah();

            Block block = engine.getBlockOrganizer().getBlockByHash(hash);

            if (block != null) {
                System.out.println("[GetDataStrategy] To send block " + hash + " from " + context.getRemoteNode().getPort());
                try {
                    Message response = new Message(
                            MessageType.BLOCK,
                            block,
                            context.getPeer().getHybridLogicalClock()
                    );
                    MessageUtils.sendMessage(context.getOutputStream(), response);
                }catch (Exception ex){
                    System.err.println("[GetDataStrategy] Error to send message  " + ex.getMessage());
                }
            }
        }
    }
}
