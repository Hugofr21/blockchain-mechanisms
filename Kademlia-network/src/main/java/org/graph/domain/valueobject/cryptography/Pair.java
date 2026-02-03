package org.graph.domain.valueobject.cryptography;

import java.util.Objects;

public record Pair<K, V>(K key, V value) {


    @Override
    public String toString() {
        return "Pair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Pair<?, ?>(Object key1, Object value1))) return false;
        return (Objects.equals(key, key1))
                && (Objects.equals(value, value1));
    }
}
