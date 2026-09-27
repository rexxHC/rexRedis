package com.rexredis.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataStoreTest {

    private DataStore store;

    @BeforeEach
    void setUp() {
        store = new DataStore();
    }

    @Test
    void testSetAndGet() {
        store.set("greeting", RedisValue.string("hello"));
        RedisValue val = store.get("greeting");
        assertThat(val).isNotNull();
        assertThat(val.getType()).isEqualTo(RedisValue.Type.STRING);
        assertThat(val.asString()).isEqualTo("hello");
    }

    @Test
    void testDeleteAndExists() {
        store.set("k1", RedisValue.string("v1"));
        assertThat(store.exists("k1")).isTrue();

        boolean deleted = store.delete("k1");
        assertThat(deleted).isTrue();
        assertThat(store.exists("k1")).isFalse();
        assertThat(store.get("k1")).isNull();
    }

    @Test
    void testKeysAndSize() {
        store.set("a", RedisValue.string("1"));
        store.set("b", RedisValue.string("2"));
        store.set("c", RedisValue.string("3"));

        assertThat(store.size()).isEqualTo(3);
        assertThat(store.keys()).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void testClear() {
        store.set("a", RedisValue.string("1"));
        store.clear();

        assertThat(store.size()).isEqualTo(0);
        assertThat(store.keys()).isEmpty();
    }
}
