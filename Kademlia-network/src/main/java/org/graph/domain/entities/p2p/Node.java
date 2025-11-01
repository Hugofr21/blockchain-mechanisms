package org.graph.domain.entities.p2p;

import java.util.Objects;

public class Node {
    private NodeId id;
    private String host;
    private int port;

    public Node(String host, int port) {
        this.id = new NodeId();
        this.host = host;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public NodeId getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node that = (Node) o;
        return port == that.port && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
