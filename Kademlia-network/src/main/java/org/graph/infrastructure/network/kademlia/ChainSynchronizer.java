package org.graph.infrastructure.network.kademlia;

import org.graph.domain.application.block.Block;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.infrastructure.blockchain.BlockchainEngine;
import org.graph.infrastructure.p2p.ConnectionHandler;
import org.graph.infrastructure.p2p.Peer;

import java.io.Serializable;

public class ChainSynchronizer {

    private final BlockchainEngine blockchain;
    private final Peer myPeer;

    public ChainSynchronizer(BlockchainEngine blockchain, Peer myPeer) {
        this.blockchain = blockchain;
        this.myPeer = myPeer;
    }
    /**
     * Inicia o processo de sincronização com um vizinho recém-conectado.
     * Deve ser chamado logo após o Handshake.
     */
    public void requestSync(ConnectionHandler handler) {
        try {
            // Envia um pedido leve apenas para saber a altura do vizinho
            Message statusReq = new Message(MessageType.GET_STATUS, "STATUS_REQ");
            handler.sendMessage(statusReq);
        } catch (Exception e) {
            System.err.println("[SYNC] Falha ao pedir status: " + e.getMessage());
        }
    }

    /**
     * Processa a resposta de status do vizinho e decide se precisamos baixar blocos.
     */
    public void handleStatusResponse(ConnectionHandler handler, StatusPayload remoteStatus) {
        int myHeight = blockchain.getBlockOrganizer().getChainHeight();
        int remoteHeight = remoteStatus.chainHeight;

        System.out.println("[SYNC] Local Height: " + myHeight + " | Remote Height: " + remoteHeight);

        // Se o vizinho tem uma cadeia maior, precisamos baixar o que falta
        if (remoteHeight > myHeight) {
            System.out.println("[SYNC] Detectada cadeia maior. Iniciando download...");

            // Loop simples para pedir bloco a bloco (pode ser otimizado com batch request)
            for (int i = myHeight + 1; i <= remoteHeight; i++) {
                try {
                    // Pede o bloco pelo número (Height/Index)
                    Message getBlockMsg = new Message(MessageType.GET_BLOCK, i);
                    handler.sendMessage(getBlockMsg);

                    // Pequeno delay para não congestionar a rede (opcional)
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("[SYNC] Cadeia já está sincronizada ou à frente.");
        }
    }

    /**
     * Responde a um pedido de status vindo de outro nó.
     */
    public void respondToStatusRequest(ConnectionHandler handler) {
        try {
            int myHeight = blockchain.getBlockOrganizer().getChainHeight();
            String bestHash = blockchain.getBlockOrganizer().getLastBlock() != null
                    ? blockchain.getBlockOrganizer().getLastBlock().getCurrentBlockHash()
                    : "0";

            StatusPayload payload = new StatusPayload(myHeight, bestHash);
            handler.sendMessage(new Message(MessageType.STATUS_RESPONSE, payload));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Responde a um pedido de bloco específico.
     */
    public void respondToBlockRequest(ConnectionHandler handler, int blockNumber) {
        try {
            Block block = blockchain.getBlockOrganizer().getBlockByNumber(blockNumber);
            if (block != null) {
                // Envia o bloco inteiro
                handler.sendMessage(new Message(MessageType.BLOCK, block));
                System.out.println("[SYNC] Enviando Bloco #" + blockNumber + " para " + handler.getRemoteNode().getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Payload auxiliar para troca de status
    public static class StatusPayload implements Serializable {
        public int chainHeight;
        public String lastHash;

        public StatusPayload(int h, String lh) {
            this.chainHeight = h;
            this.lastHash = lh;
        }
    }

}
