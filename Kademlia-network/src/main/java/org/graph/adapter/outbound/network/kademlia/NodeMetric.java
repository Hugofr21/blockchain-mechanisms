package org.graph.adapter.outbound.network.kademlia;

import org.graph.domain.entities.node.Node;

public record NodeMetric(Node node, double newDistance) {
}
