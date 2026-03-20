package org.graph.domain.entities.message;

import org.graph.infrastructure.networkTime.HybridLogicalClock;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable, Comparable<Message> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final MessageType type;
    private final HybridLogicalClock timestamp;
    private final Object payload;
    private final String requestId;
    private final String correlationId;

    public Message(MessageType type, Object objectToSerialize, HybridLogicalClock timestamp) {
        this.type = type;
        this.timestamp = timestamp;
        this.requestId = UUID.randomUUID().toString();
        this.correlationId = null;
        try {
            this.payload = SerializationUtils.serialize(objectToSerialize);
        } catch (IOException e) {
            throw new RuntimeException("[ERROR] Serialization error while creating the message.", e);
        }
    }

    public Message(MessageType type, Object objectToSerialize, HybridLogicalClock timestamp, String correlationId) {
        this.type = type;
        this.timestamp = timestamp;
        this.requestId = UUID.randomUUID().toString();
        this.correlationId = correlationId;
        try {
            this.payload = SerializationUtils.serialize(objectToSerialize);
        } catch (IOException e) {
            throw new RuntimeException("[ERROR] Serialization error while creating the message.", e);
        }
    }

    public String getId() { return requestId; }

    public String getCorrelationId() { return correlationId; }

    public boolean isResponse() { return correlationId != null; }

    public Object getPayload() { return payload; }

    public HybridLogicalClock getTimestamp() { return timestamp; }

    public MessageType getType() { return type; }

    @Override
    public int compareTo(Message o) { return 0; }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", requestId='" + requestId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}