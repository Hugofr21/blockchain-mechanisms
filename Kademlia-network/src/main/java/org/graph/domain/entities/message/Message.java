package org.graph.domain.entities.message;

import org.graph.infrastructure.utils.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable, Comparable<Message> {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    private long timestamp;
    private Object payload;
    private String id;

    public Message(MessageType type, byte[] payload) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.payload = payload;
        this.id = UUID.randomUUID().toString();
    }

    public Message(MessageType type, Object objectToSerialize) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.id = UUID.randomUUID().toString();
        try {
            this.payload = SerializationUtils.serialize(objectToSerialize);
        } catch (IOException e) {
            throw new RuntimeException("[ERROR] Serialization error while creating the message.", e);
        }
    }

    public String getId() {
        return id;
    }

    public Object getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public int compareTo(Message o) {
        return 0;
    }
}
