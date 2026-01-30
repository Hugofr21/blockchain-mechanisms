package org.graph.adapter.network.message.block;

import java.io.Serializable;

/**
 * <p>Send hash, compare with node to see if it's in the encapsulation list, and send a message.</p>
 */
public record InventoryPayload(InventoryType type, String hash) implements Serializable {
    public boolean getTypeInventoryBlock() {return type == InventoryType.BLOCK;}
    public boolean getTypeInventoryTransaction() {return type == InventoryType.TRANSACTION;}
    public String getInventoryHah() {return hash;}
}
