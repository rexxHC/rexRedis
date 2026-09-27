package com.rexredis.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages key expiry via lazy and active strategies.
 */
public class ExpiryManager {

    /** key → absolute expiry time in milliseconds since epoch */
    private final Map<String, Long> expiries = new ConcurrentHashMap<>();

    public void setExpiry(String key, long ttlMillis) {
        expiries.put(key, System.currentTimeMillis() + ttlMillis);
    }

    public boolean isExpired(String key) {
        Long expiry = expiries.get(key);
        return expiry != null && System.currentTimeMillis() > expiry;
    }

    /**
     * Returns remaining TTL in milliseconds.
     *
     * @return remaining ms, -1 if no expiry is set, -2 if expired/missing
     */
    public long ttlMillis(String key) {
        Long expiry = expiries.get(key);
        if (expiry == null) return -1;
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? remaining : -2;
    }

    public void removeExpiry(String key) {
        expiries.remove(key);
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
     * Active expiry cycle — sample random keys and evict expired ones.
     * Called periodically from the event loop.
     */
    public void activeExpiryCycle(DataStore store) {
        // TODO: Sample ~20 random keys with TTLs, delete expired ones
        //       If >25% were expired, run again immediately
    }
}
