package org.graph.application.usecase.blockchain;

import org.graph.adapter.outbound.network.message.block.ChainStatusPayload;
import org.graph.adapter.outbound.network.message.block.InventoryPayload;
import org.graph.adapter.outbound.network.message.block.InventoryType;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.gateway.NetworkGateway;
import org.graph.server.Peer;

import java.math.BigInteger;
import java.util.List;

/**
 * O processo de sincronização inicial decorre em três fases distintas.
 *
 * Phase1: Na primeira fase, o método startInitialSync() é invocado imediatamente após
 * o estabelecimento da ligação, solicitando ao nó vizinho o seu estado atual.
 *
 * Phase2: Na segunda fase, handleStatusRequest() é responsável por tratar pedidos de
 * estado recebidos de outros nós, respondendo com a informação correspondente
 * ao topo da cadeia local.
 *
 * Phase3: Na terceira fase, handleStatusResponse() processa o estado recebido do vizinho
 * e decide se é necessário descarregar blocos adicionais para colmatar diferenças.
 *
 *                      Important for the system
 * Quando é identificado um bloco em falta, o mecanismo recoverMissingBlock
 * deve ser acionado. Nesse cenário, é necessário registar no
 * BlockEventManager.handleBlock() um callback específico para blocos em falta,
 * permitindo que a recuperação seja efetuada através de procura na rede
 * Kademlia.
 */

public class ChainSyncUseCase {
    private final NetworkGateway gateway;
    private final IEventDispatcher dispatcher;
    private Peer myself;
    public ChainSyncUseCase(NetworkGateway gateway, IEventDispatcher dispatcher, Peer myself) {
        this.gateway = gateway;
        this.dispatcher = dispatcher;
        this.myself = myself;
    }



    public void startInitialSync(ConnectionHandler handler) {
        System.out.println("[BOOTSTRAP] Requesting chain status....");
        Message msg = new Message(MessageType.GET_STATUS, null, handler.getPeer().getHybridLogicalClock());
        dispatcher.sendUnicast(msg, handler);
    }

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

    public void handleStatusResponse(ChainStatusPayload remoteStatus, ConnectionHandler source) {
        String remoteHash = remoteStatus.bestBlockHash();
        int remoteHeight = remoteStatus.blockHeight();

        var lastBlock = gateway.getBlockchainEngine().getBlockOrganizer().getLastBlock();

        int localHeight = (lastBlock != null) ? lastBlock.getNumberBlock() : -1;
        String localHash = (lastBlock != null) ? lastBlock.getCurrentBlockHash() : "";

        boolean isBehind = remoteHeight > localHeight;
        boolean isForkAtSameHeight = (localHeight == remoteHeight && localHeight >= 0 && !localHash.equals(remoteHash));

        if (isBehind || isForkAtSameHeight) {
            int requestFrom = localHeight;

            if (isForkAtSameHeight) {
                System.out.println("[SYNC] Fork detected at the same time (" + localHeight + "). Hashes diverge!");
                requestFrom = localHeight - 1;
            } else {
                System.out.println("[SYNC] Delay detected. Requesting blocks starting at height: " + localHeight);
            }

            requestFrom = Math.max(-1, requestFrom);

            InventoryPayload req = new InventoryPayload(InventoryType.BATCH_REQUEST, String.valueOf(requestFrom));
            Message msg = new Message(MessageType.GET_BLOCKS_BATCH, req, source.getPeer().getHybridLogicalClock());
            dispatcher.sendUnicast(msg, source);

        } else {
            System.out.println("[SYNC] Node is already synchronized with the remote branch.");
        }
    }

    public void recoverMissingBlock(String missingHash) {
        new Thread(() -> {
            try {
                System.out.println("[KADEMLIA] Searching for orphaned block in DHT: " + missingHash);
                BigInteger targetKey = new BigInteger(missingHash, 16);
                Block recoveredBlock = myself.getMkademliaNetwork().findValue(targetKey, Block.class);
                if (recoveredBlock != null) {
                    System.out.println("[KADEMLIA] Block retrieved successfully!");
                    myself.getNetworkGateway().processIncomingBlock(recoveredBlock);
                } else {
                    System.out.println("[KADEMLIA] Block not found on the network.");
                }
            } catch (Exception e) {
                System.err.println("[KADEMLIA] Error retrieving: " + e.getMessage());
            }
        }).start();
    }


    public void handleBlockBatch(List<Block> batch, ConnectionHandler source) {
        System.out.println("[SYNC] Batch received from " + batch.size() + " blocks.");

        for (Block b : batch) {
            gateway.processIncomingBlock(b);
        }

        if (batch.size() == 20) {
            int newHeight = gateway.getBlockchainEngine().getBlockOrganizer().getChainHeight() - 1;
            InventoryPayload req = new InventoryPayload(InventoryType.BATCH_REQUEST, String.valueOf(newHeight));
            Message msg = new Message(MessageType.GET_BLOCKS_BATCH, req, source.getPeer().getHybridLogicalClock());
            dispatcher.sendUnicast(msg, source);
        } else {
            System.out.println("[SYNC] Batch synchronization completed.");
        }
    }
}