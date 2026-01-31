package org.graph.adapter.network.message.node;


import java.io.Serializable;
import java.util.List;

public record NodeListPayload(List<NodeInfoPayload> nodes) implements Serializable {
}