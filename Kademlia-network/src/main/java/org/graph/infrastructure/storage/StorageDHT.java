package org.graph.infrastructure.storage;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class StorageDHT {
    private ReentrantLock lock;
    private Map<BigInteger, StoredValue> data;

    private record StoredValue(Object value, Type type) {

    }

    public <T> void put(BigInteger id, T value, Type type) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(value);
        Objects.requireNonNull(type);
        data.put(id, new StoredValue(value, type));
    }

    public void put(BigInteger id, Object value) {
        put(id, value, value.getClass());
    }

    public <T> T get(BigInteger id, Class<T> clazz) {
        StoredValue sv = data.get(id);
        if (sv == null) return null;
        if (!clazz.isAssignableFrom((Class<?>)sv.type)) {
            throw new ClassCastException("Stored type " + sv.type + " is not assignable to " + clazz);
        }
        return clazz.cast(sv.value);
    }




}
