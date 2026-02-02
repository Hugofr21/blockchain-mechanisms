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
import org.graph.server.Peer;

import java.math.BigInteger;

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

public class ChainSyncManager {
    private final NetworkGateway gateway;
    private final IEventDispatcher dispatcher;
    private Peer myself;
    public ChainSyncManager(NetworkGateway gateway, IEventDispatcher dispatcher, Peer myself) {
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
        System.out.println("LAST BLOCK: " + lastBlock);
        int localHeight = (lastBlock != null) ? lastBlock.getNumberBlock() : -1;

        System.out.println("[SYNC] Local Height: " + localHeight + " | Remote Height: " + remoteHeight);

        boolean needsSync = (remoteHeight > localHeight);

        if (!needsSync && localHeight == remoteHeight && lastBlock != null) {
            if (!lastBlock.getCurrentBlockHash().equals(remoteHash)) {
                needsSync = true;
                System.out.println("[SYNC] Hash mismatch at the same time (Fork detected).");
            }
        }

        if (needsSync) {
            System.out.println("[SYNC] Delay detected. Requesting top block: " + remoteHash);
            sendGetData(remoteHash, source);
        } else {
            System.out.println("[SYNC] Node is now synchronized.");
        }
    }


    public void recoverMissingBlock(String missingHash) {
        BigInteger targetKey = new BigInteger(missingHash, 16);
        Object result = myself.getMkademliaNetwork().findValue(targetKey);

        if (result != null && result instanceof Block recoveredBlock) {
            System.out.println("[KADEMLIA] Block successfully recovered!");
            myself.getNetworkGateway().processIncomingBlock(recoveredBlock);
        } else {
            System.out.println("[KADEMLIA] Block not found on the network.");
        }
    }

    private void sendGetData(String hash, ConnectionHandler target) {
        InventoryPayload req = new InventoryPayload(InventoryType.BLOCK, hash);
        Message msg = new Message(MessageType.GET_BLOCK, req, target.getPeer().getHybridLogicalClock());
        dispatcher.sendUnicast(msg, target);
    }
}