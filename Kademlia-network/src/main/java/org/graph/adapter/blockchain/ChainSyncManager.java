package org.graph.adapter.blockchain;

import org.graph.adapter.network.message.block.ChainStatusPayload;
import org.graph.adapter.network.message.block.InventoryPayload;
import org.graph.adapter.network.message.block.InventoryType;
import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.gateway.NetworkGateway;

public class ChainSyncManager {
    private final NetworkGateway gateway;
    private final IEventDispatcher dispatcher;

    public ChainSyncManager(NetworkGateway gateway, IEventDispatcher dispatcher) {
        this.gateway = gateway;
        this.dispatcher = dispatcher;
    }

    /**
     * Passo 1: Assim que conecta, pede o estado.
     */
    public void startInitialSync(ConnectionHandler handler) {
        System.out.println("[BOOTSTRAP] Solicitando estado da cadeia...");
        Message msg = new Message(MessageType.GET_STATUS, null, handler.getPeer().getHybridLogicalClock());
        dispatcher.sendUnicast(msg, handler);
    }


    /**
     * Passo 2: Alguém pediu meu estado. Respondo com meu Topo.
     */
    public void handleStatusRequest(ConnectionHandler requester) {
        Block myTop = gateway.getBlockchainEngine().getBlockOrganizer().getLastBlock();

        ChainStatusPayload status = new ChainStatusPayload(
                myTop.getCurrentBlockHash(),
                myTop.getNumberBlock()
        );

        Message response = new Message(
                MessageType.CHAIN_STATUS_RESPONSE,
                status,
                requester.getPeer().getHybridLogicalClock()
        );

        dispatcher.sendUnicast(response, requester);
    }

    /**
     * Passo 3: Recebe o estado do vizinho e decide se baixa blocos.
     */
    public void handleStatusResponse(ChainStatusPayload remoteStatus, ConnectionHandler source) {
        String remoteHash = remoteStatus.bestBlockHash();
        int remoteHeight = remoteStatus.blockHeight();

        // --- CORREÇÃO DO NULL POINTER ---
        // Se o nó acabou de nascer, getLastBlock() é null.
        // Definimos altura como -1 para garantir que ele baixe o Genesis (altura 0).
        var lastBlock = gateway.getBlockchainEngine().getBlockOrganizer().getLastBlock();
        System.out.println("LAST BLOCK: " + lastBlock);
        int localHeight = (lastBlock != null) ? lastBlock.getNumberBlock() : -1;

        System.out.println("[SYNC] Local Height: " + localHeight + " | Remote Height: " + remoteHeight);

        // Se o remoto tem uma cadeia maior
        // OU se têm a mesma altura mas hashes diferentes (Fork/Conflito)
        boolean needsSync = (remoteHeight > localHeight);

        if (!needsSync && localHeight == remoteHeight && lastBlock != null) {
            // Verifica se o hash bate
            if (!lastBlock.getCurrentBlockHash().equals(remoteHash)) {
                needsSync = true;
                System.out.println("[SYNC] Hash mismatch na mesma altura (Fork detectado).");
            }
        }

        if (needsSync) {
            System.out.println("[SYNC] Detectado atraso. Pedindo bloco de topo: " + remoteHash);
            // Inicia o download reverso
            sendGetData(remoteHash, source);
        } else {
            System.out.println("[SYNC] Nó já está sincronizado.");
        }
    }

    private void sendGetData(String hash, ConnectionHandler target) {
        InventoryPayload req = new InventoryPayload(InventoryType.BLOCK, hash);
        Message msg = new Message(MessageType.GET_BLOCK, req, target.getPeer().getHybridLogicalClock());
        dispatcher.sendUnicast(msg, target);
    }
}