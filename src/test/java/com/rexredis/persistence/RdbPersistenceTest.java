package com.rexredis.persistence;

import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RdbPersistenceTest {

    private DataStore sourceStore;
    private DataStore restoredStore;
    private RdbSaver saver;
    private RdbLoader loader;

    @BeforeEach
    void setUp() {
        sourceStore = new DataStore();
        restoredStore = new DataStore();
        saver = new RdbSaver();
        loader = new RdbLoader();
    }

    @Test
    void testSaveAndLoadAllDataTypes(@TempDir Path tempDir) throws IOException {
        Path rdbPath = tempDir.resolve("dump.rdb");

        // 1. Populate source store
        sourceStore.set("greeting", RedisValue.string("hello world"));
        sourceStore.set("numbers", RedisValue.list(List.of("one", "two", "three")));
        sourceStore.set("colors", RedisValue.set(Set.of("red", "green", "blue")));
        sourceStore.set("user:1", RedisValue.hash(Map.of("name", "Rex", "role", "admin")));

        // Set expiry on greeting (1 hour in future)
        sourceStore.getExpiryManager().setExpiry("greeting", 3600_000);

        // 2. Save
        saver.save(sourceStore, rdbPath.toString());
        assertThat(Files.exists(rdbPath)).isTrue();
        assertThat(Files.size(rdbPath)).isGreaterThan(0);

        // 3. Load into restored store
        loader.load(rdbPath.toString(), restoredStore);

        // 4. Verify contents
        assertThat(restoredStore.size()).isEqualTo(4);

        // String
        assertThat(restoredStore.get("greeting").asString()).isEqualTo("hello world");
        assertThat(restoredStore.getExpiryManager().hasExpiry("greeting")).isTrue();
        assertThat(restoredStore.getExpiryManager().ttlSeconds("greeting", restoredStore)).isGreaterThan(3500);

        // List
        assertThat(restoredStore.get("numbers").asList()).containsExactly("one", "two", "three");

        // Set
        assertThat(restoredStore.get("colors").asSet()).containsExactlyInAnyOrder("red", "green", "blue");

        // Hash
        assertThat(restoredStore.get("user:1").asHash()).containsEntry("name", "Rex").containsEntry("role", "admin");
    }

    @Test
    void testExpiredKeysSkippedOnLoad(@TempDir Path tempDir) throws IOException {
        Path rdbPath = tempDir.resolve("dump_expiry.rdb");

        sourceStore.set("alive", RedisValue.string("survivor"));
        sourceStore.set("alreadyDead", RedisValue.string("ghost"));

        // Set absolute expiry in past for alreadyDead
        sourceStore.getExpiryManager().setAbsoluteExpiry("alreadyDead", System.currentTimeMillis() - 1000);
        // Alive key has no expiry
        saver.save(sourceStore, rdbPath.toString());

        loader.load(rdbPath.toString(), restoredStore);

        assertThat(restoredStore.exists("alive")).isTrue();
        assertThat(restoredStore.exists("alreadyDead")).isFalse();
    }

    @Test
    void testInvalidMagicHeaderThrowsException(@TempDir Path tempDir) throws IOException {
        Path badPath = tempDir.resolve("bad_magic.rdb");
        Files.write(badPath, new byte[]{ 'B', 'A', 'D', 'M', 'A', 'G', 'I', 'C', 1, 0, 0, 0, 0, (byte) 0xFF });

        assertThatThrownBy(() -> loader.load(badPath.toString(), restoredStore))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid RDB magic header");
    }

    @Test
    void testCorruptedEofThrowsException(@TempDir Path tempDir) throws IOException {
        Path badPath = tempDir.resolve("bad_eof.rdb");
        // Valid header and 0 keys, but invalid EOF marker (0x00 instead of 0xFF)
        byte[] content = new byte[]{
                'R', 'E', 'X', 'R', 'E', 'D', 'I', 'S',
                1,              // version 1
                0, 0, 0, 0,     // 0 keys
                0x00            // bad EOF marker
        };
        Files.write(badPath, content);

        assertThatThrownBy(() -> loader.load(badPath.toString(), restoredStore))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("missing EOF marker");
    }

    @Test
    void testKeyExpiringMidSaveNotLoaded(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path rdbPath = tempDir.resolve("dump_mid_expire.rdb");

        sourceStore.set("k1", RedisValue.string("v1"));
        sourceStore.getExpiryManager().setExpiry("k1", 200);

        saver.save(sourceStore, rdbPath.toString());

        Thread.sleep(300);

        loader.load(rdbPath.toString(), restoredStore);
        assertThat(restoredStore.exists("k1")).isFalse();
    }
}
