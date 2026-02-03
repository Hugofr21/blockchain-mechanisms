package org.graph.infrastructure.storage.povider;

public interface LRU<K, V> {

    void writeToCache(K key, V value);

    V readFromCache(K key);

}