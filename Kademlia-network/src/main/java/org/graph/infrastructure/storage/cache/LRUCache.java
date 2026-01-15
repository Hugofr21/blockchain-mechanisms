package org.graph.infrastructure.storage.cache;

// https://medium.com/@vishal637yadav/least-recently-used-lru-cache-implementation-in-java-7460346ffe73
// Least Recently Used (LRU) Cache


import org.graph.infrastructure.storage.povider.LRU;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRUCache<K, V> implements LRU<K, V> {
    private static final int DEFAULT_CACHE_SIZE = 100;

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Node(" + key + ", " + value + ", Prev-" + (prev != null ? prev.key : null) +
                    ", Next-" + (next != null ? next.key : null) + ")";
        }
    }

    private Node<K, V> head;
    private Node<K, V> tail;
    private final Map<K, Node<K, V>> nodeMap;
    private final int cacheSize;


    public LRUCache() {
        cacheSize = DEFAULT_CACHE_SIZE;
        nodeMap = new ConcurrentHashMap<>();
    }


    private void addToFront(Node<K, V> node) {
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

    private synchronized Node<K, V> removeFromTail() {
        Node<K, V> removedNode = tail;

        tail.prev.next = null;
        tail = tail.prev;

        return nodeMap.remove(removedNode.key);
    }


    private synchronized void moveToHead(Node<K, V> node) {
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
           Node<K, V> node = nodeMap.get(key);
           node.value = value;
           moveToHead(node);
           return;
       }

        Node<K, V> node = new Node<>(key, value);
        addToFront(node);

        if (nodeMap.size() > cacheSize) {
            Node<K, V> removedNode = removeFromTail();
            System.out.println("Remove From Tail!!--------{}" + removedNode.toString());
        }
    }

    @Override
    public synchronized V readFromCache(K key) {
        if (key == null) {
           return null;
        }
        Node<K, V> node = nodeMap.get(key);
        moveToHead(node);
        return null;
    }

    private synchronized  Map<K, Node<K, V>> getNodeMap() {
        return nodeMap;
    }

    public synchronized boolean containsKey(K key) {
        return nodeMap.containsKey(key);
    }
}