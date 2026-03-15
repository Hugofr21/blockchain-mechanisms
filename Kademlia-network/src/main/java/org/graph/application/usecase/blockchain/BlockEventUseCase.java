package org.graph.application.usecase.blockchain;

import org.graph.adapter.outbound.network.message.block.InventoryPayload;
import org.graph.adapter.outbound.network.message.block.InventoryType;
import org.graph.domain.entities.node.Node;
import org.graph.domain.entities.transaction.Transaction;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.gateway.NetworkGateway;
import org.graph.gateway.block.BlockStateRemote;

import java.util.ArrayList;
import java.util.List;

/**
 * O BlockEventManager atua como camada central de validação e coordenação
 * no tratamento de eventos relacionados com blocos recebidos da rede.
 *
 * Esta componente é responsável por validar a conformidade estrutural dos
 * dados recebidos, verificar a integridade da cadeia e determinar a resposta
 * apropriada em cada situação: aceitar o bloco, solicitar blocos em falta
 * ou rejeitar dados inconsistentes.
 *
 * Caso um bloco seja recebido sem que o seu bloco pai esteja disponível,
 * o bloco deve ser marcado como órfão. Nessa situação, é iniciado um pedido
 * explícito ao par correspondente para obtenção do bloco pai em falta.
 *
 * O comportamento do gestor varia consoante o tipo de evento recebido:
 *
 * CENÁRIO 1: Receção de um anúncio (INV).
 * Um nó vizinho anuncia a disponibilidade de um determinado bloco.
 * O sistema avalia se o bloco já existe localmente e, caso contrário,
 * decide se deve solicitar o seu conteúdo.
 *
 * CENÁRIO 2: Receção de um pedido (GET_BLOCK / GET_DATA).
 * Um nó vizinho solicita explicitamente um bloco existente.
 * O sistema valida a disponibilidade do bloco e procede ao seu envio,
 * respeitando as regras de sincronização e segurança.
 *
 * CENÁRIO 3: Receção de um bloco (BLOCK).
 * O bloco é recebido na íntegra e submetido a validações estruturais,
 * criptográficas e de encadeamento antes de ser integrado na cadeia
 * ou armazenado temporariamente como bloco órfão.
 */

public class BlockEventUseCase {
    private final NetworkGateway gateway;
    private final IEventDispatcher dispatcher;

    public BlockEventUseCase(NetworkGateway gateway, IEventDispatcher dispatcher) {
        this.gateway = gateway;
        this.dispatcher = dispatcher;
    }

    public void handleInv(InventoryPayload payload, ConnectionHandler source) {
        String hash = payload.hash();


        if (payload.type() == InventoryType.BLOCK) {
            if (!gateway.getBlockchainEngine().getBlockOrganizer().contains(hash)) {
                System.out.println("[SYNC] Unknown Block Hash Announced: " + hash.substring(0, 8) + "... Requesting data.");
                sendGetData(hash, source, InventoryType.BLOCK);
            }
        }

        else if (payload.type() == InventoryType.TRANSACTION) {
            if (!gateway.getBlockchainEngine().getTransactionOrganizer().isTransactionKnown(hash)) {
                System.out.println("[GOSSIP] Unknown Tx hash announced: " + hash.substring(0, 8) + "... Requesting data.");
                sendGetData(hash, source, InventoryType.TRANSACTION);
            }
        }
    }

    public void handleGetData(InventoryPayload payload, ConnectionHandler requester) {
        String hash = payload.hash();

        if (payload.type() == InventoryType.BLOCK) {
            Block block = gateway.getBlockchainEngine().getBlockOrganizer().getBlockByHash(hash);
            if (block != null) {
                System.out.println("[UPLOAD] Sending block " + hash.substring(0, 8) + " to " + requester.getRemoteNode().getPort());
                Message response = new Message(MessageType.BLOCK, block, requester.getPeer().getHybridLogicalClock());
                dispatcher.sendUnicast(response, requester);
            }
        }

        else if (payload.type() == InventoryType.TRANSACTION) {
            Transaction tx = gateway.getBlockchainEngine().getTransactionOrganizer().getTransactionById(hash);
            if (tx != null) {
                System.out.println("[UPLOAD] Sending transaction " + hash.substring(0, 8) + " to " + requester.getRemoteNode().getPort());
                Message response = new Message(MessageType.TRANSACTION, tx, requester.getPeer().getHybridLogicalClock());
                dispatcher.sendUnicast(response, requester);
            }
        }
    }

    public void handleBlock(Block block, ConnectionHandler source) {
        BlockStateRemote result = gateway.processIncomingBlock(block);

        switch (result) {
            case ADDED -> {
                System.out.println("[SYNC] Block " + block.getNumberBlock() + " added! Propagating inventory...");
                propagateBlockInv(block, source);
                if (source.getRemoteNode() != null) {
                    gateway.getMyself().getReputationsManager().reportEvent(
                            source.getRemoteNode().getNodeId().value(),
                            EventTypePolicy.VALID_BLOCK
                    );
                }
            }
            case MISSING_PARENT -> {
                System.out.println("[SYNC] Gap detected in block " + block.getNumberBlock() + ". Requesting parent...");
                String parentHash = block.getHeader().getPreviousBlockHash();
                sendGetData(parentHash, source, InventoryType.BLOCK);

                if (source.getPeer().getmChainSyncController() != null) {
                    source.getPeer().getmChainSyncController().recoverMissingBlock(parentHash);
                }
            }
            case INVALID -> {
                System.err.println("[SYNC] Invalid block received.");
                gateway.getMyself().getReputationsManager().reportEvent(source.getRemoteNode().getNodeId().value(), EventTypePolicy.INVALID_BLOCK);
            }
            case EXISTS -> System.out.println("[DEBUG] Block already exists in the chain: " + block.getCurrentBlockHash());
            case ORPHAN -> System.err.println("[WARNING] Block stored as orphan: " + block.getCurrentBlockHash());
        }
    }

    private void sendGetData(String hash, ConnectionHandler target, InventoryType type) {
        InventoryPayload req = new InventoryPayload(type, hash);
        Message msg = new Message(MessageType.GET_BLOCK, req, target.getPeer().getHybridLogicalClock());
        dispatcher.sendUnicast(msg, target);
    }
    public void propagateBlockInv(Block block, ConnectionHandler excludeSource) {
        InventoryPayload inv = new InventoryPayload(InventoryType.BLOCK, block.getCurrentBlockHash());
        Message msg = new Message(MessageType.INV_DATA, inv, excludeSource != null ? excludeSource.getPeer().getHybridLogicalClock() : gateway.getMyself().getHybridLogicalClock());

        Node excludeNode = excludeSource != null ? excludeSource.getRemoteNode() : null;
        dispatcher.broadcastExcept(msg, excludeNode);
    }

    public void propagateTransactionInv(Transaction tx, ConnectionHandler excludeSource) {
        InventoryPayload inv = new InventoryPayload(InventoryType.TRANSACTION, tx.getTxId());
        Message msg = new Message(MessageType.INV_DATA, inv, excludeSource != null ? excludeSource.getPeer().getHybridLogicalClock() : gateway.getMyself().getHybridLogicalClock());

        Node excludeNode = excludeSource != null ? excludeSource.getRemoteNode() : null;
        dispatcher.broadcastExcept(msg, excludeNode);
    }


    public void handleGetBlocksBatch(InventoryPayload payload, ConnectionHandler requester) {
        if (payload.type() != InventoryType.BATCH_REQUEST) return;

        try {
            int startHeight = Integer.parseInt(payload.hash());
            List<Block> myChain = gateway.getBlockchainEngine().getBlockOrganizer().getOrderedChain();

            List<Block> batchToSend = new ArrayList<>();
            for (Block b : myChain) {
                if (b.getNumberBlock() > startHeight) {
                    batchToSend.add(b);
                    if (batchToSend.size() >= 20) break;
                }
            }

            if (!batchToSend.isEmpty()) {
                System.out.println("[UPLOAD] Sending batch of " + batchToSend.size() + " blocks to " + requester.getRemoteNode().getPort());
                Message response = new Message(MessageType.BLOCK_BATCH, batchToSend, requester.getPeer().getHybridLogicalClock());
                dispatcher.sendUnicast(response, requester);
            }
        } catch (NumberFormatException e) {
            System.err.println("[SYNC] Invalid batch request: " + payload.hash());
        }
    }
}
