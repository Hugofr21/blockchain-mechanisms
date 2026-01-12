package org.graph.domain.entities.message;

public enum MessageType {
    PING((byte) 0x01),
    PONG((byte) 0x02),
    STORAGE((byte) 0x03),
    FIND_NODE((byte) 0x04),
    FIND_VALUE((byte) 0x05),

    BLOCK((byte) 0x10),
    CHUNK((byte) 0x11),
    REQUEST((byte) 0x12),
    ACK((byte) 0x13),
    HELLO((byte) 0x14);

    private final byte code;

    MessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static MessageType fromCode(byte code) {
        for (MessageType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown message type: " + code);
    }
}
