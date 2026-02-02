package org.graph.adapter.storage;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class StorageDHT {
    private Map<BigInteger, StoredValue> data;
    private final ReentrantLock lock;

    public StorageDHT(){
        this.data = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    private record StoredValue(Object value, Class<?> type) {}

    public void put(BigInteger id, Object value) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(value);

        lock.lock();
        try {
            data.put(id, new StoredValue(value, value.getClass()));
        } finally {
            lock.unlock();
        }
    }

    public <T> T get(BigInteger id, Class<T> clazz) {
        lock.lock();
        try {
            StoredValue sv = data.get(id);
            if (sv == null) return null;
            if (!clazz.isAssignableFrom(sv.type)) {
                System.err.println("[Storage] Type error: Expected " + clazz.getSimpleName() +
                        " but found " + sv.type.getSimpleName());
                return null;
            }
            return clazz.cast(sv.value);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try { return data.size(); } finally { lock.unlock(); }
    }

}
