package org.graph.adapter.network.message.block.data;

import org.graph.adapter.blockchain.BlockchainEngine;
import org.graph.adapter.network.message.block.InventoryPayload;
import org.graph.adapter.network.message.block.InventoryType;
import org.graph.adapter.network.message.block.provider.MessageStrategy;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;

public class InvStrategy implements MessageStrategy {
    @Override
    public void handle(Message message, ConnectionHandler context) {
        InventoryPayload payload = (InventoryPayload) message.getPayload();
        BlockchainEngine engine = context.getPeer().getNetworkGateway().getBlockchainEngine();

        if (payload.type() == InventoryType.BLOCK) {
            String hash = payload.hash();

            // TODO: VERIFICAÇÃO RÁPIDA (Cache/Storage)
            if (!engine.getBlockOrganizer().contains(hash)) {
                System.out.println("[GOSSIP] Novo bloco anunciado: " + hash + ". Solicitando dados...");
                try {
                    InventoryPayload requestPayload = new InventoryPayload(InventoryType.BLOCK, hash);
                    Message request = new Message(
                            MessageType.GET_BLOCK,
                            requestPayload,
                            context.getPeer().getHybridLogicalClock()
                    );

                    MessageUtils.sendMessage(context.getOutputStream(), request);
                }catch (Exception ex){
                    System.err.println(ex.getMessage());
                }
            }
        }

    }
}
