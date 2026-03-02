package org.graph.infrastructure.utils;

import org.graph.adapter.outbound.network.message.node.FindNodePayload;
import org.graph.adapter.outbound.network.message.node.NodeListPayload;
import org.graph.adapter.utils.Base64Utils;

import java.math.BigInteger;

public final class  EncapsulationUtils {
    public static BigInteger decapsulationNodeId(Object rawPayload){
        try {

            if (rawPayload instanceof BigInteger) {
                return (BigInteger) rawPayload;
            }


            if (rawPayload instanceof String hexString) {
                return new BigInteger(hexString, 16);
            }

            FindNodePayload payloadObj = null;

            if (rawPayload instanceof FindNodePayload) {
                payloadObj = (FindNodePayload) rawPayload;
            }
            else if (rawPayload instanceof byte[] data) {
                Object deserialized = SerializationUtils.deserialize(data);
                if (deserialized instanceof FindNodePayload) {
                    payloadObj = (FindNodePayload) deserialized;
                } else if (deserialized instanceof BigInteger) {
                    return (BigInteger) deserialized;
                }
            }

            if (payloadObj != null) {
                String base64Str = payloadObj.targetIdBase64();
                byte[] decodedBytes = Base64Utils.decodeToBytes(base64Str);
                return new BigInteger(1, decodedBytes);
            }

        } catch (Exception ex){
            System.out.println("[ERROR] Decapsulation for node id: " + ex.getMessage());
        }

        return null;
    }

    public  static NodeListPayload  decapsulationListNodes(Object rawPayload){

        if (rawPayload instanceof NodeListPayload payload) {
            return payload;
        }

        if (rawPayload instanceof byte[] data) {
            try {
                Object obj = SerializationUtils.deserialize(data);
                if (obj instanceof NodeListPayload payload) {
                    return  payload;
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to deserialize bytes from the list:" + e.getMessage());
            }
        }
        return null;
    }

}
