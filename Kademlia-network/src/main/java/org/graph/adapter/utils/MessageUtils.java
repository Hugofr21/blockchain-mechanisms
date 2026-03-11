package org.graph.adapter.utils;

import org.graph.adapter.outbound.network.message.network.HandshakePayload;
import org.graph.domain.entities.message.Message;
import org.graph.infrastructure.crypt.SecureSession;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import static org.graph.adapter.utils.Constants.MAX_MESSAGE_SIZE;

public final class MessageUtils {

    public static Message readMessage(DataInputStream inputStream) throws IOException, ClassNotFoundException {
        synchronized (inputStream) {
            int length = inputStream.readInt();

            if (length <= 0 || length > MAX_MESSAGE_SIZE * 2) {
                throw new IOException("[MESSAGE_UTILS] Invalid message size (Stream corrupted): " + length);
            }

            byte[] raw = new byte[length];
            inputStream.readFully(raw);
            Object obj = SerializationUtils.deserialize(raw);

            if (!(obj instanceof Message)) {
                throw new IOException("[MESSAGE_UTILS] Invalid message type received.");
            }

            return (Message) obj;
        }
    }

    public static void sendMessage(DataOutputStream outputStream, Object object) throws IOException {
        byte[] data = SerializationUtils.serialize(object);
        synchronized (outputStream) {
            outputStream.writeInt(data.length);
            outputStream.write(data);
            outputStream.flush();
        }
    }

    public static void sendSecureMessage(DataOutputStream outputStream, Object object, SecureSession session) throws Exception {
        byte[] rawData = SerializationUtils.serialize(object);
        byte[] encryptedPackage = session.encrypt(rawData);
        synchronized (outputStream) {
            outputStream.writeInt(encryptedPackage.length);
            outputStream.write(encryptedPackage);
            outputStream.flush();
        }
    }

    public static Message readSecureMessage(DataInputStream inputStream, SecureSession session) throws Exception {

        synchronized (inputStream) {
            int length = inputStream.readInt();

            if (length <= 0 || length > org.graph.adapter.utils.Constants.MAX_MESSAGE_SIZE * 2) {
                throw new IOException("[SECURITY] Invalid message size in encrypted tunnel.: " + length);
            }

            byte[] encryptedPackage = new byte[length];
            inputStream.readFully(encryptedPackage);

            byte[] rawData = session.decrypt(encryptedPackage);

            Object obj = SerializationUtils.deserialize(rawData);

            if (!(obj instanceof Message)) {
                throw new IOException("[SECURITY] Invalid message type after decryption..");
            }

            return (Message) obj;
        }
    }

    public static HandshakePayload extractHandshakePayload(Message response, Logger logger) {
        Object rawPayload = response.getPayload();
        try {
            if (rawPayload instanceof HandshakePayload) {
                return (HandshakePayload) rawPayload;
            } else if (rawPayload instanceof byte[]) {
                return (HandshakePayload) SerializationUtils.deserialize((byte[]) rawPayload);
            } else {
                logger.severe("Unexpected handshake payload type.");
                return null;
            }
        } catch (Exception e) {
            logger.severe("Failed to deserialize HandshakePayload: " + e.getMessage());
            return null;
        }
    }


    public static byte[] extractSignature(Object rawAckPayload, Logger logger) {
        try {
            if (rawAckPayload instanceof byte[]) {
                try {
                    Object deserialized = SerializationUtils.deserialize((byte[]) rawAckPayload);
                    if (deserialized instanceof byte[]) {
                        return (byte[]) deserialized;
                    } else if (deserialized instanceof HandshakePayload) {
                        return ((HandshakePayload) deserialized).signature();
                    } else {
                        return (byte[]) rawAckPayload;
                    }
                } catch (Exception e) {
                    return (byte[]) rawAckPayload;
                }
            } else if (rawAckPayload instanceof HandshakePayload) {
                return ((HandshakePayload) rawAckPayload).signature();
            } else {
                logger.severe("Invalid HELLO_ACK payload type.");
                return null;
            }
        } catch (Exception e) {
            logger.severe("Error parsing ACK payload: " + e.getMessage());
            return null;
        }
    }
}
