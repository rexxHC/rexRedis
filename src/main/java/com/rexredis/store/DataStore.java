package com.rexredis.store;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central key-value data store.
 * Maps String keys to typed RedisValue objects.
 */
public class DataStore {

    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();
    private final ExpiryManager expiryManager = new ExpiryManager();

    public RedisValue get(String key) {
        if (expiryManager.isExpired(key)) {
            store.remove(key);
            expiryManager.removeExpiry(key);
            return null;
        }
        return store.get(key);
    }

    public void set(String key, RedisValue value) {
        expiryManager.removeExpiry(key);
        store.put(key, value);
    }

    public boolean delete(String key) {
        expiryManager.removeExpiry(key);
        return store.remove(key) != null;
    }

    public boolean exists(String key) {
        if (expiryManager.isExpired(key)) {
            store.remove(key);
            expiryManager.removeExpiry(key);
            return false;
        }
        return store.containsKey(key);
    }

    public Set<String> keys() {
        Set<String> activeKeys = new HashSet<>();
        for (String key : store.keySet()) {
            if (exists(key)) {
                activeKeys.add(key);
            }
        }
        return activeKeys;
    }

    public int size() {
        return keys().size();
    }

    public void clear() {
        store.clear();
        expiryManager.clear();
    }

    public ExpiryManager getExpiryManager() {
        return expiryManager;
    }

    public Map<String, RedisValue> getAll() {
        return store;
    }
}
