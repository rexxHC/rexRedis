package com.rexredis.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central key-value data store.
 * Maps String keys to typed RedisValue objects.
 *
 * <p>Thread safety: All data operations run on the single event-loop thread,
 * so no concurrent data structures are needed. The only cross-thread interaction
 * is Pub/Sub fan-out, which is handled via {@code ClientHandler.sendResponse()}
 * using its own synchronization.
 */
public class DataStore {

    private final Map<String, RedisValue> store = new HashMap<>();
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
        List<String> expiredKeys = new ArrayList<>();
        for (String key : store.keySet()) {
            if (!expiryManager.isExpired(key)) {
                activeKeys.add(key);
            } else {
                expiredKeys.add(key);
            }
        }
        // Clean up expired keys after iteration to avoid ConcurrentModificationException
        for (String key : expiredKeys) {
            store.remove(key);
            expiryManager.removeExpiry(key);
        }
        return activeKeys;
    }

    public int size() {
        int count = 0;
        for (String key : store.keySet()) {
            if (!expiryManager.isExpired(key)) {
                count++;
            }
        }
        return count;
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
