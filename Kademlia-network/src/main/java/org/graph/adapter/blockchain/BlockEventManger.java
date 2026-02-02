package org.graph.adapter.blockchain;

import org.graph.adapter.network.message.block.InventoryPayload;
import org.graph.adapter.network.message.block.InventoryType;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.gateway.NetworkGateway;
import org.graph.gateway.block.BlockStateRemote;

public class BlockEventManger {
    private final NetworkGateway gateway;
    private final IEventDispatcher dispatcher;

    public BlockEventManger(NetworkGateway gateway, IEventDispatcher dispatcher) {
        this.gateway = gateway;
        this.dispatcher = dispatcher;
    }

    /**
     * CENÁRIO 1: Recebo um ANÚNCIO (INV)
     * "O vizinho diz que tem o bloco X."
     */
    public void handleInv(InventoryPayload payload, ConnectionHandler source) {
        if (payload.type() != InventoryType.BLOCK) return;

        String hash = payload.hash();

        if (!gateway.getBlockchainEngine().getBlockOrganizer().contains(hash)) {
            System.out.println("[SYNC] Hash desconhecido anunciado: " + hash + ". Pedindo dados...");
            sendGetData(hash, source);
        }
    }

    /**
     * CENÁRIO 2: Recebo um PEDIDO (GET_BLOCK/GET_DATA)
     * "O vizinho quer o bloco X."
     */
    public void handleGetData(InventoryPayload payload, ConnectionHandler requester) {
        if (!payload.getTypeInventoryBlock()) return;

        String hash = payload.getInventoryHah();
        Block block = gateway.getBlockchainEngine().getBlockOrganizer().getBlockByHash(hash);

        if (block != null) {
            System.out.println("[UPLOAD] Enviando bloco " + hash + " para " + requester.getRemoteNode().getPort());

            // Enviamos o bloco inteiro (Java Serialization lida com o tamanho via TCP Stream)
            Message response = new Message(MessageType.BLOCK, block, requester.getPeer().getHybridLogicalClock());
            dispatcher.sendUnicast(response, requester);
        }
    }

    /**
     * CENÁRIO 3: Recebo o BLOCO (BLOCK)
     * "O bloco chegou (pode ser grande)."
     */
    /**
     * CENÁRIO 3: Recebo o BLOCO (BLOCK)
     */
    public void handleBlock(Block block, ConnectionHandler source) {
        BlockStateRemote result = gateway.processIncomingBlock(block);

        switch (result) {
            case ADDED -> {
                System.out.println("[SYNC] Bloco " + block.getNumberBlock() + " Added! Spreading the word...");
                propagateInv(block, source);
            }

            case MISSING_PARENT -> {
                System.out.println("[SYNC] Gap detected in the block " + block.getNumberBlock());
                System.out.println("[SYNC] Requesting PAI block: " + block.getHeader().getPreviousBlockHash());

                sendGetData(block.getHeader().getPreviousBlockHash(), source);
            }

            case INVALID -> System.err.println("[SYNC] Invalid block of " + source.getRemoteNode().getPort());

            case EXISTS -> {
                System.out.println("[DEBUG] This block to find inside chain " + block.getCurrentBlockHash());
            }
            case ORPHAN -> {
                System.err.println("[WARNING] This block is orphan " + block.getCurrentBlockHash());
            }
        }
    }

    private void sendGetData(String hash, ConnectionHandler target) {
        InventoryPayload req = new InventoryPayload(InventoryType.BLOCK, hash);
        Message msg = new Message(MessageType.GET_BLOCK, req, target.getPeer().getHybridLogicalClock());
        dispatcher.sendUnicast(msg,target);
    }

    private void propagateInv(Block block, ConnectionHandler excludeSource) {
        InventoryPayload inv = new InventoryPayload(InventoryType.BLOCK, block.getCurrentBlockHash());
        Message msg = new Message(MessageType.INV_DATA, inv, excludeSource.getPeer().getHybridLogicalClock());
        dispatcher.broadcastExcept(msg, excludeSource.getRemoteNode());
    }
}
