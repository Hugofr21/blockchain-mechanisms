package org.graph.domain.common;

public class Pair<K,V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public V getValue() {return value;}
    public K getKey() {return key;}


    @Override
    public String toString() {
        return "Pair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }

    @Override
    public int hashCode() {
        int result = (key == null ? 0 : key.hashCode());
        result = 31 * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Pair)) return false;
        Pair<?, ?> other = (Pair<?, ?>) obj;
        return (key == null ? other.key == null : key.equals(other.key))
                && (value == null ? other.value == null : value.equals(other.value));
    }
}
