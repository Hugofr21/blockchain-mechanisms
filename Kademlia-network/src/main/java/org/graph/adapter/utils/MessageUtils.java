package org.graph.adapter.utils;

import org.graph.domain.entities.message.Message;
import org.graph.infrastructure.utils.SerializationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
}
