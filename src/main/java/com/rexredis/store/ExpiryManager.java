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
     * Uses random index sampling instead of copying/shuffling the full key set.
     */
    public void activeExpiryCycle(DataStore store) {
        if (expiries.isEmpty()) {
            return;
        }

        for (int loop = 0; loop < ACTIVE_EXPIRE_MAX_LOOPS; loop++) {
            // Take a snapshot of keys as an array for random access
            Object[] keyArray = expiries.keySet().toArray();
            if (keyArray.length == 0) {
                break;
            }

            int countToSample = Math.min(keyArray.length, ACTIVE_EXPIRE_KEYS_PER_LOOP);
            int expiredCount = 0;
            java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();

            // Use Fisher-Yates partial shuffle to pick countToSample unique random keys
            for (int i = 0; i < countToSample; i++) {
                int j = rng.nextInt(i, keyArray.length);
                Object tmp = keyArray[i];
                keyArray[i] = keyArray[j];
                keyArray[j] = tmp;

                String key = (String) keyArray[i];
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
