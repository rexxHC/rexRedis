package com.rexredis.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiryManagerTest {

    private DataStore store;
    private ExpiryManager expiryManager;

    @BeforeEach
    void setUp() {
        store = new DataStore();
        expiryManager = store.getExpiryManager();
    }

    @Test
    void testSetExpiryAndIsExpired() throws InterruptedException {
        store.set("tempKey", RedisValue.string("val"));
        expiryManager.setExpiry("tempKey", 50); // 50ms

        assertThat(expiryManager.isExpired("tempKey")).isFalse();
        assertThat(store.get("tempKey")).isNotNull();

        Thread.sleep(70);

        assertThat(expiryManager.isExpired("tempKey")).isTrue();
        // Lazy expiration via get()
        assertThat(store.get("tempKey")).isNull();
        assertThat(store.exists("tempKey")).isFalse();
    }

    @Test
    void testTtlReturnCodes() {
        // -2 if key does not exist
        assertThat(expiryManager.ttlMillis("nonexistent", store)).isEqualTo(-2);
        assertThat(expiryManager.ttlSeconds("nonexistent", store)).isEqualTo(-2);

        // -1 if key exists without TTL
        store.set("persisted", RedisValue.string("forever"));
        assertThat(expiryManager.ttlMillis("persisted", store)).isEqualTo(-1);
        assertThat(expiryManager.ttlSeconds("persisted", store)).isEqualTo(-1);

        // Positive remaining time if TTL is set
        expiryManager.setExpiry("persisted", 10000); // 10s
        assertThat(expiryManager.ttlMillis("persisted", store)).isGreaterThan(0).isLessThanOrEqualTo(10000);
        assertThat(expiryManager.ttlSeconds("persisted", store)).isGreaterThan(0).isLessThanOrEqualTo(10);
    }

    @Test
    void testRemoveExpiry() {
        store.set("k1", RedisValue.string("v1"));
        expiryManager.setExpiry("k1", 5000);
        assertThat(expiryManager.hasExpiry("k1")).isTrue();

        boolean removed = expiryManager.removeExpiry("k1");
        assertThat(removed).isTrue();
        assertThat(expiryManager.hasExpiry("k1")).isFalse();
        assertThat(expiryManager.ttlSeconds("k1", store)).isEqualTo(-1);
    }

    @Test
    void testActiveExpiryCycle() throws InterruptedException {
        // Add 30 keys that expire quickly
        for (int i = 0; i < 30; i++) {
            String key = "bulkExp:" + i;
            store.set(key, RedisValue.string("val" + i));
            expiryManager.setExpiry(key, 30);
        }

        // Add 5 keys that don't expire
        for (int i = 0; i < 5; i++) {
            store.set("keep:" + i, RedisValue.string("val" + i));
        }

        Thread.sleep(50);

        // Run active eviction cycle
        expiryManager.activeExpiryCycle(store);

        // All 30 expired keys should have been actively swept
        for (int i = 0; i < 30; i++) {
            assertThat(store.getAll().containsKey("bulkExp:" + i)).isFalse();
        }

        // 5 kept keys should still exist
        for (int i = 0; i < 5; i++) {
            assertThat(store.exists("keep:" + i)).isTrue();
        }
    }
}
