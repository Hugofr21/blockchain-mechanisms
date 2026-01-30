package org.graph.adapter.network.message.block.data;

import org.graph.adapter.network.message.block.InventoryPayload;
import org.graph.adapter.network.message.block.InventoryType;
import org.graph.adapter.network.message.block.provider.MessageStrategy;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.p2p.Peer;
import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;

public class BlockStrategy implements MessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        Block block = (Block) message.getPayload();
        Peer peer = context.getPeer();
        boolean isNewAndValid = peer.getNetworkGateway().incomingBlock(block);
        if (isNewAndValid) {
            System.out.println("[GOSSIP] Block validated. Propagating INV to neighbors...");
            InventoryPayload inv = new InventoryPayload(InventoryType.BLOCK, block.getCurrentBlockHash());
            Message invMsg = new Message(
                    MessageType.INV_DATA,
                    inv,
                    peer.getHybridLogicalClock()
            );

            peer.getNeighboursManager().broadcastExcept(invMsg, context.getRemoteNode());
        }
    }
}
