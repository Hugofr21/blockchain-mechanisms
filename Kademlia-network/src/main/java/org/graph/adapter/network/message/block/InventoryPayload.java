package org.graph.adapter.network.message.block;

import java.io.Serializable;
import  java.io.Serial;
/**
 * <p>Send hash, compare with node to see if it's in the encapsulation list, and send a message.</p>
 */
public record InventoryPayload(InventoryType type, String hash) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public boolean getTypeInventoryBlock() { return type == InventoryType.BLOCK; }
    public String getInventoryHah() { return hash; }
}