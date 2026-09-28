package com.rexredis.store;

import java.util.*;

/**
 * Manages key expiry via lazy and active strategies.
 *
 * <p>Thread safety: Accessed exclusively from the event-loop thread.
 * No concurrent data structures required.
 */
public class ExpiryManager {

    private static final int ACTIVE_EXPIRE_KEYS_PER_LOOP = 20;
    private static final int ACTIVE_EXPIRE_MAX_LOOPS = 10;

    /** key → absolute expiry time in milliseconds since epoch */
    private final Map<String, Long> expiries = new HashMap<>();
    private final java.util.ArrayList<String> keyList = new java.util.ArrayList<>();
    private final java.util.Map<String, Integer> idx = new java.util.HashMap<>();

    public void setExpiry(String key, long ttlMillis) {
        setAbsoluteExpiry(key, System.currentTimeMillis() + ttlMillis);
    }

    public void setAbsoluteExpiry(String key, long timestampMillis) {
        if (!expiries.containsKey(key)) {
            idx.put(key, keyList.size());
            keyList.add(key);
        }
        expiries.put(key, timestampMillis);
    }

    public boolean isExpired(String key) {
        Long expiry = expiries.get(key);
        return expiry != null && System.currentTimeMillis() > expiry;
    }

    public long ttlMillis(String key) {
        Long expiry = expiries.get(key);
        if (expiry == null) return -1;
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? remaining : -2;
    }

    public long ttlSeconds(String key) {
        long ms = ttlMillis(key);
        if (ms < 0) return ms;
        return (ms + 999) / 1000;
    }

    /**
     * Returns remaining TTL in milliseconds according to Redis specification:
     * - returns -2 if the key does not exist.
     * - returns -1 if the key exists but has no associated expire.
     * - returns remaining milliseconds otherwise.
     */
    public long ttlMillis(String key, DataStore store) {
        if (!store.exists(key)) {
            return -2;
        }
        Long expiry = expiries.get(key);
        if (expiry == null) {
            return -1;
        }
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? remaining : -2;
    }

    /**
     * Returns remaining TTL in seconds according to Redis specification:
     * - returns -2 if the key does not exist.
     * - returns -1 if the key exists but has no associated expire.
     * - returns remaining seconds otherwise.
     */
    public long ttlSeconds(String key, DataStore store) {
        long ms = ttlMillis(key, store);
        if (ms < 0) {
            return ms;
        }
        return (ms + 999) / 1000;
    }

    public boolean removeExpiry(String key) {
        if (expiries.remove(key) != null) {
            int removeIdx = idx.remove(key);
            int lastIdx = keyList.size() - 1;
            if (removeIdx != lastIdx) {
                String lastKey = keyList.get(lastIdx);
                keyList.set(removeIdx, lastKey);
                idx.put(lastKey, removeIdx);
            }
            keyList.remove(lastIdx);
            return true;
        }
        return false;
    }

    public boolean hasExpiry(String key) {
        return expiries.containsKey(key);
    }

    public Long getExpiry(String key) {
        return expiries.get(key);
    }

    public Map<String, Long> getAllExpiries() {
        return expiries;
    }

    public void clear() {
        expiries.clear();
        keyList.clear();
        idx.clear();
    }

    /**
     * Active expiry cycle — sample random keys with TTLs and evict expired ones.
     * Uses random index sampling instead of copying/shuffling the full key set.
     */
    public void activeExpiryCycle(DataStore store) {
        if (keyList.isEmpty()) {
            return;
        }

        for (int loop = 0; loop < ACTIVE_EXPIRE_MAX_LOOPS; loop++) {
            if (keyList.isEmpty()) {
                break;
            }

            int countToSample = Math.min(keyList.size(), ACTIVE_EXPIRE_KEYS_PER_LOOP);
            int expiredCount = 0;
            java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();

            for (int i = 0; i < countToSample; i++) {
                String key = keyList.get(rng.nextInt(keyList.size()));
                if (isExpired(key)) {
                    store.delete(key);
                    expiredCount++;
                }
            }

            // If <= 25% were expired, stop the cycle
            if (countToSample == 0 || (expiredCount * 100 / countToSample) <= 25) {
                break;
            }
        }
    }
}
