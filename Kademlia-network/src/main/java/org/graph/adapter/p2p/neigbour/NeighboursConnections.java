package org.graph.adapter.p2p.neigbour;

import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.adapter.p2p.Peer;
import org.graph.domain.entities.message.Message;
import org.graph.domain.entities.message.MessageType;
import org.graph.domain.entities.p2p.Node;
import org.graph.adapter.utils.MessageUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NeighboursConnections {
    private final Peer peer;
    private final Map<BigInteger, Long> lastTimestamp;
    public final Map<BigInteger, ConnectionEntry> nodesActives;
    private final static long TIME_LIMIT_FAIL_CONNECTIONS = 45000L; // 45s: Considerado morto
    private final static long TIME_TO_SEND_PING = 20000L;           // 20s: Enviar PING de verificação
    private final static long CHECK_INTERVAL = 5000L;               // 5s: Intervalo da verificação
    private final ScheduledExecutorService scheduler;

    public NeighboursConnections(Peer peer) {
        this.peer = peer;
        this.lastTimestamp = new ConcurrentHashMap<>();
        this.nodesActives = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }


    public void start() {
        // Executa verifyHeartbeat a cada 5 segundos
        scheduler.scheduleAtFixedRate(this::verifyHeartbeat,
                CHECK_INTERVAL,
                CHECK_INTERVAL,
                TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * Regista uma nova conexão ativa.
     * Deve ser chamado quando um handshake TCP é completado.
     */
    public void addConnection(Node node, ConnectionHandler handler) {
        BigInteger id = node.getNodeId().value();
        nodesActives.put(id, new ConnectionEntry(node, handler));
        updateTimestamp(id);
        System.out.println("[HEARTBEAT] Nó registado para monitorização: " + id);
    }

    /**
     * Atualiza o timestamp de um nó.
     * Deve ser chamado sempre que qualquer mensagem é recebida (ConnectionHandler).
     */
    public void updateTimestamp(BigInteger nodeId) {
        lastTimestamp.put(nodeId, System.currentTimeMillis());
    }

    /**
     * Remove explicitamente uma conexão (ex: logout ou erro de IO).
     */
    public void removeConnection(BigInteger nodeId) {
        ConnectionEntry entry = nodesActives.remove(nodeId);
        lastTimestamp.remove(nodeId);

        if (entry != null && entry.handler() != null) {
            entry.handler().closeConnection();
        }
    }

    /**
     * Lógica central do Heartbeat.
     * Verifica timeouts e envia PINGS proativos.
     */
    private void verifyHeartbeat() {
        long now = System.currentTimeMillis();

        for (BigInteger nodeId : nodesActives.keySet()) {
            Long lastSeen = lastTimestamp.get(nodeId);
            if (lastSeen == null) {
                updateTimestamp(nodeId);
                continue;
            }

            long diff = now - lastSeen;

            if (diff > TIME_LIMIT_FAIL_CONNECTIONS) {
                System.out.println("[HEARTBEAT] Nó morto detetado (Timeout): " + nodeId);

                removeConnection(nodeId);

                Node deadNode = peer.getRoutingTable().getByNodeIdNode(nodeId);
                if (deadNode != null) {
                    peer.getRoutingTable().removeNode(deadNode);
                }

            } else if (diff > TIME_TO_SEND_PING) {
                System.out.println("[HEARTBEAT] Nó silencioso. Enviando PING para: " + nodeId);
                sendKeepAlivePing(nodeId);
            }
        }
    }

    private void sendKeepAlivePing(BigInteger nodeId) {
        ConnectionEntry entry = nodesActives.get(nodeId);
        if (entry != null && entry.handler() != null) {
            try {
                // Envia PING usando a conexão TCP já aberta
                Message pingMsg = new Message(MessageType.PING, "KEEP_ALIVE", peer.getHybridLogicalClock().next());
                MessageUtils.sendMessage(entry.handler().getOutputStream(), pingMsg);
            } catch (IOException e) {
                System.out.println("[HEARTBEAT] Falha ao enviar PING: " + e.getMessage());
                // Se falhar o envio, provavelmente o socket caiu. Removemos na próxima iteração ou agora.
                removeConnection(nodeId);
            }
        }
    }

    public List<Node> getActiveNeighbours() {
        return nodesActives.values().stream()
                .map(ConnectionEntry::node)
                .collect(Collectors.toList());
    }

    public Node getNeighbourById(BigInteger id) {
        ConnectionEntry entry = nodesActives.get(id);
        return entry != null ? entry.node() : null;
    }

    public boolean isNodeConnected(BigInteger nodeId) {
        return nodesActives.containsKey(nodeId);
    }
}
