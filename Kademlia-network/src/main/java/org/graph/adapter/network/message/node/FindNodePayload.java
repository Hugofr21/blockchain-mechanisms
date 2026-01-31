package org.graph.adapter.network.message.node;

import java.io.Serializable;

public record FindNodePayload(String targetIdBase64) implements Serializable {
}
