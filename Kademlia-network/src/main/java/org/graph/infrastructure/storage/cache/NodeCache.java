package org.graph.infrastructure.storage.cache;


public class NodeCache<K, V> {
    K key;
    V value;
    NodeCache<K, V> prev;
    NodeCache<K, V> next;

    public NodeCache(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Node(" + key + ", " + value + ", Prev-" + (prev != null ? prev.key : null) +
                ", Next-" + (next != null ? next.key : null) + ")";
    }
}