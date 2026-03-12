package org.graph.adapter.inbound.network;

import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.policy.EventTypePolicy;
import org.graph.server.Peer;
import org.graph.domain.entities.node.Node;
import java.math.BigInteger;


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

public class HeartbeatEvent implements Runnable {
    private final Peer myself;
    private static final long TIME_TO_SEND_PING = 10_000;
    private static final long TIME_LIMIT_FAIL_CONNECTIONS = 30_000;

    public HeartbeatEvent(Peer myself) {
        this.myself = myself;
    }

    @Override
    public void run() {
        if (!myself.getIsRunning()) return;

        long now = System.currentTimeMillis();

        for (BigInteger nodeId : myself.getNeighboursManager().getActivesNeighborsByBigInteger()) {
            Long lastSeen = myself.getNeighboursManager().getLastTimeByNodeId(nodeId);

            if (lastSeen == null) {
                myself.getNeighboursManager().updateTimestamp(nodeId);
                continue;
            }

            long diff = now - lastSeen;

            if (diff > TIME_LIMIT_FAIL_CONNECTIONS) {
                myself.getLogger().info("[HEARTBEAT] Ghost node detected (Timeout: " + diff + "ms). Purging: " + nodeId);
                purgeDeadNode(nodeId);
            } else if (diff > TIME_TO_SEND_PING) {
                triggerAsyncPing(nodeId);
            }
        }
    }

    private void purgeDeadNode(BigInteger nodeId) {
        myself.getNeighboursManager().removeConnection(nodeId);

        Node deadNode = myself.getRoutingTable().getByNodeIdNode(nodeId);
        if (deadNode != null) {
            myself.getRoutingTable().removeNode(deadNode);
        }

         myself.getReputationsManager().reportEvent(nodeId, EventTypePolicy.ABRUPT_DISCONNECT);
    }

    private void triggerAsyncPing(BigInteger nodeId) {
        Node target = myself.getNeighboursManager().getNeighbourByIdNode(nodeId);
        if (target != null) {
            Message pingMsg = new Message(MessageType.PING, System.currentTimeMillis(), myself.getHybridLogicalClock());
            myself.getMkademliaNetwork().sendRPCAsync(target, pingMsg);
        }
    }
}