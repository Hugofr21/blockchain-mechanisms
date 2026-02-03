package org.graph.adapter.storage.cache;

// https://medium.com/@vishal637yadav/least-recently-used-lru-cache-implementation-in-java-7460346ffe73
// Least Recently Used (LRU) Cache


import org.graph.adapter.storage.povider.LRU;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRUCache<K, V> implements LRU<K, V> {
    private static final int DEFAULT_CACHE_SIZE = 100;
    private NodeCache<K, V> head;
    private NodeCache<K, V> tail;
    private final Map<K, NodeCache<K, V>> nodeMap;
    private final int cacheSize;


    public LRUCache() {
        cacheSize = DEFAULT_CACHE_SIZE;
        nodeMap = new ConcurrentHashMap<>();
    }

    private void addToFront(NodeCache<K, V> node) {
       nodeMap.put(node.key, node);
       if(head == null) {
           head = node;
           tail = node;
       }else  {
           node.prev = head;
           head.prev =  node;
           head = node;
       }

    }

    private synchronized NodeCache<K, V> removeFromTail() {
        NodeCache<K, V> removedNode = tail;

        tail.prev.next = null;
        tail = tail.prev;

        return nodeMap.remove(removedNode.key);
    }


    private synchronized void moveToHead(NodeCache<K, V> node) {
        if (head == node) {
            return;
        }

        if (head == tail) {
            tail = tail.prev;
            if (tail != null) { tail.next = null; }

        }else  {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        node.next = head;
        node.prev = null;
        if (head != null) head.prev = node;
        head = node;

    }

    @Override
    public synchronized void writeToCache(K key, V value) {
        if (key == null) {
           return;
        }

       if (nodeMap.containsKey(key)) {
           NodeCache<K, V> node = nodeMap.get(key);
           node.value = value;
           moveToHead(node);
           return;
       }

        NodeCache<K, V> node = new NodeCache<>(key, value);
        addToFront(node);

        if (nodeMap.size() > cacheSize) {
            NodeCache<K, V> removedNode = removeFromTail();
            System.out.println("Remove From Tail!!--------{}" + removedNode.toString());
        }
    }

    @Override
    public synchronized V readFromCache(K key) {
        if (key == null) {
           return null;
        }

        NodeCache<K, V> node = nodeMap.get(key);
        if (node != null) {
            moveToHead(node);
            return node.value;
        }
        return null;
    }

    private synchronized  Map<K, NodeCache<K, V>> getNodeMap() {
        return nodeMap;
    }

    public synchronized boolean containsKey(K key) {
        return nodeMap.containsKey(key);
    }
}