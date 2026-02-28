package org.graph.infrastructure.network.neighbor;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.domain.entities.node.Node;

public record ConnectionEntry(Node node, ConnectionHandler handler) {

}
