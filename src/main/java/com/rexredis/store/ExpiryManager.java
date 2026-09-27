package com.rexredis.store;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages key expiry via lazy and active strategies.
 */
public class ExpiryManager {

    private static final int ACTIVE_EXPIRE_KEYS_PER_LOOP = 20;
    private static final int ACTIVE_EXPIRE_MAX_LOOPS = 10;

    /** key → absolute expiry time in milliseconds since epoch */
    private final Map<String, Long> expiries = new ConcurrentHashMap<>();

    public void setExpiry(String key, long ttlMillis) {
        setAbsoluteExpiry(key, System.currentTimeMillis() + ttlMillis);
    }

    public void setAbsoluteExpiry(String key, long timestampMillis) {
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
        return expiries.remove(key) != null;
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
    }

    /**
     * Active expiry cycle — sample random keys with TTLs and evict expired ones.
     * Follows Redis active expiration algorithm:
     * 1. Sample up to 20 keys with expiries.
     * 2. Evict expired keys.
     * 3. If >25% were expired, repeat immediately (up to ACTIVE_EXPIRE_MAX_LOOPS times).
     */
    public void activeExpiryCycle(DataStore store) {
        if (expiries.isEmpty()) {
            return;
        }

        for (int loop = 0; loop < ACTIVE_EXPIRE_MAX_LOOPS; loop++) {
            List<String> candidateKeys = new ArrayList<>(expiries.keySet());
            if (candidateKeys.isEmpty()) {
                break;
            }

            Collections.shuffle(candidateKeys);
            int countToSample = Math.min(candidateKeys.size(), ACTIVE_EXPIRE_KEYS_PER_LOOP);
            int expiredCount = 0;

            for (int i = 0; i < countToSample; i++) {
                String key = candidateKeys.get(i);
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
