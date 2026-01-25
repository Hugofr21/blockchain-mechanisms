package org.graph.domain.application.kademlia;

import org.graph.domain.entities.p2p.Node;

public record NodeMetric(Node node, double newDistance) {
}
