package org.graph.message;

import java.io.Serializable;
import java.rmi.server.UID;
import java.util.UUID;

public class Message implements Serializable, Comparable<Message> {
    private MessageType type;
    private long timestamp;
    private String payload;
    private String header;
    private String id;

    public Message(MessageType type, String payload, String header) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.payload = payload;
        this.header = header;
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getHeader() {
        return header;
    }

    public String getPayload() {
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
