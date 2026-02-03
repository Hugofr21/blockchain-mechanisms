package org.graph.infrastructure.network.neigbour;

import org.graph.infrastructure.network.ConnectionHandler;
import org.graph.domain.entities.node.Node;

public record ConnectionEntry(Node node, ConnectionHandler handler) {

}
