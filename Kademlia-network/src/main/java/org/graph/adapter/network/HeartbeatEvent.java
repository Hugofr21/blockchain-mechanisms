package org.graph.adapter.network;

import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.server.Peer;
import org.graph.adapter.utils.MessageUtils;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;

import java.io.IOException;
import java.math.BigInteger;
import java.util.logging.Logger;

import static org.graph.adapter.utils.Constants.*;


/**
 * Mecanismo de deteção periódica do estado de uma máquina WETA na rede.
 *
 * Para verificar se uma host se encontra ainda ativa, são enviadas
 * mensagens vazias de forma periódica. O conteúdo da mensagem é irrelevante,
 * uma vez que o objetivo exclusivo é confirmar a disponibilidade e o estado
 * de conectividade da máquina.
 *
 * Caso a máquina deixe de responder, considera-se que não se encontra ativa,
 * sendo então removida da tabela de encaminhamento (routable), evitando
 * tentativas de comunicação com máquinas inativas ou potencialmente
 * comprometidas, como máquinas zumbi.
 *
 * Este mecanismo aplica-se igualmente a situações em que a máquina deixa de
 * comunicar durante um período prolongado de tempo, sendo tal comportamento
 * interpretado como perda de conectividade ou falha operacional.
 */

public class HeartbeatEvent implements Runnable{
   Peer myself;
   Logger logger;

   public HeartbeatEvent(Peer myself){
       this.myself = myself;
       this.logger = myself.getLogger();
   }

    @Override
    public void run() {

        while (myself.getIsRunning()){
            try {
                verifyHeartbeat();
                Thread.sleep(CHECK_INTERVAL);
            }catch (InterruptedException exception) {
                logger.severe("[HEARTBEAT] Error to send message from the node: " + exception.getMessage());
            }
        }

    }



    private void verifyHeartbeat() {
        long now = System.currentTimeMillis();

        for (BigInteger nodeId : myself.getNeighboursManager().getActivesNeighborsByBigInteger()) {
            Long lastSeen = myself.getNeighboursManager().getLastTimeByNodeId(nodeId);
            if (lastSeen == null) {
                myself.getNeighboursManager().updateTimestamp(nodeId);
                continue;
            }

            long diff = now - lastSeen;

            if (diff > TIME_LIMIT_FAIL_CONNECTIONS) {
                System.out.println("[HEARTBEAT] Nó morto detetado (Timeout): " + nodeId);

                myself.getNeighboursManager().removeConnection(nodeId);

                Node deadNode = myself.getRoutingTable().getByNodeIdNode(nodeId);
                if (deadNode != null) {
                    myself.getRoutingTable().removeNode(deadNode);
                }

            } else if (diff > TIME_TO_SEND_PING) {
                System.out.println("[HEARTBEAT] Silent knot. Sending PING to: " + nodeId);
                sendKeepAlivePing(nodeId, myself);
            }
        }
    }

    private void sendKeepAlivePing(BigInteger nodeId, Peer myself) {
        ConnectionHandler context = myself.getNeighboursManager().getNeighbourById(nodeId);
        if (context != null ) {
            Node target = myself.getNeighboursManager().getNeighbourByIdNode(nodeId);
            boolean isAlive = myself.getMkademliaNetwork().ping(target);
            if (isAlive) {
                myself.getNeighboursManager().updateTimestamp(nodeId);
                System.out.println("[HEARTBEAT] PONG received from" + nodeId);
            } else {
                System.out.println("[HEARTBEAT] Ping failed to reach " + nodeId + ". It will be removed in the next cycle if it persists.");
            }

        }
    }

}
