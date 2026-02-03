package org.graph.domain.application.usecase.blockchain;

import org.graph.adapter.outbound.network.message.block.InventoryPayload;
import org.graph.adapter.outbound.network.message.block.InventoryType;
import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.adapter.provider.IEventDispatcher;
import org.graph.domain.entities.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.gateway.NetworkGateway;
import org.graph.gateway.block.BlockStateRemote;

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

public class BlockEventManger {
    private final NetworkGateway gateway;
    private final IEventDispatcher dispatcher;

    public BlockEventManger(NetworkGateway gateway, IEventDispatcher dispatcher) {
        this.gateway = gateway;
        this.dispatcher = dispatcher;
    }


    public void handleInv(InventoryPayload payload, ConnectionHandler source) {
        if (payload.type() != InventoryType.BLOCK) return;

        String hash = payload.hash();

        if (!gateway.getBlockchainEngine().getBlockOrganizer().contains(hash)) {
            System.out.println("[SYNC] Hash unknown announce: " + hash + ". Request data...");
            sendGetData(hash, source);
        }
    }


    public void handleGetData(InventoryPayload payload, ConnectionHandler requester) {
        if (!payload.getTypeInventoryBlock()) return;

        String hash = payload.getInventoryHah();
        Block block = gateway.getBlockchainEngine().getBlockOrganizer().getBlockByHash(hash);

        if (block != null) {
            System.out.println("[UPLOAD] To send block " + hash + " para " + requester.getRemoteNode().getPort());

            Message response = new Message(MessageType.BLOCK, block, requester.getPeer().getHybridLogicalClock());
            dispatcher.sendUnicast(response, requester);
        }
    }

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
                String parentHash = block.getHeader().getPreviousBlockHash();
                sendGetData(parentHash, source);

                if (source.getPeer().getmChainSyncController() != null) {
                    source.getPeer().getmChainSyncController().recoverMissingBlock(parentHash);
                }

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
