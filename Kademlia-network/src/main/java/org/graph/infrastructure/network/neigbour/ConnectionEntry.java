package org.graph.adapter.p2p.neigbour;

import org.graph.adapter.p2p.ConnectionHandler;
import org.graph.domain.entities.node.Node;

public record ConnectionEntry(Node node, ConnectionHandler handler) {

}
