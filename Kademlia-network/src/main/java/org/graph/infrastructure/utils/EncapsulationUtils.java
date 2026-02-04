package org.graph.infrastructure.utils;

import org.graph.adapter.outbound.network.message.node.FindNodePayload;
import org.graph.adapter.outbound.network.message.node.NodeListPayload;
import org.graph.adapter.utils.Base64Utils;

import java.math.BigInteger;

public final class  EncapsulationUtils {

    public  static BigInteger encapsulationNodeId(Object rawPayload){
        try {
            BigInteger targetId;
            FindNodePayload payloadObj = null;

            if (rawPayload instanceof FindNodePayload) {
                payloadObj = (FindNodePayload) rawPayload;
            }

            else if (rawPayload instanceof byte[] data) {
                Object deserialized = SerializationUtils.deserialize(data);

                if (deserialized instanceof FindNodePayload) {
                    payloadObj = (FindNodePayload) deserialized;
                }
            }

            String base64Str = payloadObj.targetIdBase64();
            byte[] decodedBytes = Base64Utils.decodeToBytes(base64Str);
            targetId = new BigInteger(1, decodedBytes);

            return targetId;
        }catch (Exception ex){
            System.out.println("[ERROR] Decapsulation for node id: " + ex.getMessage());
        }

        return null;
    }

    public  static NodeListPayload  encapsulationListNodes(Object rawPayload){
        NodeListPayload container = null;

        if (rawPayload instanceof NodeListPayload) {
            container = (NodeListPayload) rawPayload;
        }


        else if (rawPayload instanceof byte[] data) {
            try {
                Object obj = SerializationUtils.deserialize(data);
                if (obj instanceof NodeListPayload) {
                    container = (NodeListPayload) obj;
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to deserialize bytes from the list:" + e.getMessage());
            }
        }

        return container;
    }

}
