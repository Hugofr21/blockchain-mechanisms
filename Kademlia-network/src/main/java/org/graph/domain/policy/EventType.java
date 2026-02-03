package org.graph.domain.entities.policy;

public enum EventType {
    PING_SUCCESS,
    FIND_NODE_USEFUL,
    PING_FAIL,
    INVALID_BLOCK, // -500
    INVALID_DATA
}
