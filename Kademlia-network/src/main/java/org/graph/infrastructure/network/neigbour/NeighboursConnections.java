package org.graph.infrastructure.network.neigbour;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.server.Peer;
import org.graph.domain.entities.node.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/*
 * Neste class demos compara com routable porque objetivo desta gerir thread e sockets activos
 */
public class NeighboursConnections {
    private final Map<BigInteger, Long> lastTimestamp;
    public final Map<BigInteger, ConnectionEntry> nodesActives;

    public NeighboursConnections(Peer myself) {
        this.lastTimestamp = new ConcurrentHashMap<>();
        this.nodesActives = new ConcurrentHashMap<>();
    }


    public Long getLastTimeByNodeId(BigInteger id){
        return lastTimestamp.getOrDefault(id, 0L);
    }

    public List<BigInteger> getActivesNeighborsByBigInteger(){
        return new ArrayList<>(nodesActives.keySet());
    }


    public List<Node> getActiveNeighbours() {
        return nodesActives.values().stream()
                .map(ConnectionEntry::node)
                .collect(Collectors.toList());
    }


    /**
     * Returna um thread estado activa.
     * @param nodeId Demos passar com parametro identificador do node que estamos buscar
     */

    public ConnectionHandler getNeighbourById(BigInteger nodeId) {
        ConnectionEntry entry = nodesActives.get(nodeId);
        return entry != null ? entry.handler() : null;
    }

    public Node getNeighbourByIdNode(BigInteger nodeId) {
        ConnectionEntry entry = nodesActives.get(nodeId);
        return entry != null ? entry.node() : null;
    }

    /**
     * Regista uma nova conexão ativa.
     * Deve ser chamado quando um handshake TCP é completado.
     */
    public List<ConnectionHandler> getActivesNeighbors(){
        return nodesActives.values().stream()
                .map(ConnectionEntry::handler)
                .collect(Collectors.toList());
    }

    /**
     * Regista uma nova conexão ativa.
     * @param nodeId Demos passar com parametro identificador do node que estamos buscar
     */

    public boolean isNodeConnected(BigInteger nodeId) {
        return nodesActives.containsKey(nodeId);
    }

    /**
     * Regista uma nova conexão ativa.
     * Deve ser chamado quando um RTT UDP é completado.
     * @param node
     * @param handler
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
     * @param nodeId
     */
    public void removeConnection(BigInteger nodeId) {
        ConnectionEntry entry = nodesActives.remove(nodeId);
        lastTimestamp.remove(nodeId);

        if (entry != null && entry.handler() != null) {
            entry.handler().closeConnection();
        }
    }
}
