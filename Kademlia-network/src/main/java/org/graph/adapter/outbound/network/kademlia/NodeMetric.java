package org.graph.domain.entities.network;

import org.graph.domain.entities.node.Node;

public record NodeMetric(Node node, double newDistance) {
}
