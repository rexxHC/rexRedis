package com.rexredis.integration;

import com.rexredis.config.ServerConfig;
import com.rexredis.server.RexRedisServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RexRedisIntegrationTest {

    private RexRedisServer server;
    private Thread serverThread;
    private int port;
    private Jedis jedis;

    @BeforeEach
    void setUp() throws Exception {
        ServerConfig config = new ServerConfig(0); // 0 means pick any free port
        config.setPersistenceEnabled(false);

        server = new RexRedisServer(config);
        serverThread = new Thread(server::start, "test-server-thread");
        serverThread.start();

        server.awaitRunning();
        port = server.getBoundPort();
        jedis = new Jedis("localhost", port);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (jedis != null) {
            jedis.close();
        }
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.join(2000);
        }
    }

    @Test
    void testPingDefault() {
        String pong = jedis.ping();
        assertThat(pong).isEqualTo("PONG");
    }

    @Test
    void testPingWithMessage() {
        String res = jedis.ping("custom message");
        assertThat(res).isEqualTo("custom message");
    }

    @Test
    void testEcho() {
        String echo = jedis.echo("Phase 1 Complete!");
        assertThat(echo).isEqualTo("Phase 1 Complete!");
    }

    @Test
    void testMultipleConcurrentClients() {
        try (Jedis client1 = new Jedis("localhost", port);
             Jedis client2 = new Jedis("localhost", port)) {

            assertThat(client1.ping()).isEqualTo("PONG");
            assertThat(client2.ping()).isEqualTo("PONG");
            assertThat(client1.echo("client1")).isEqualTo("client1");
            assertThat(client2.echo("client2")).isEqualTo("client2");
        }
    }

    @Test
    void testStringCommands() {
        assertThat(jedis.set("framework", "rexredis")).isEqualTo("OK");
        assertThat(jedis.get("framework")).isEqualTo("rexredis");
        assertThat(jedis.get("missing")).isNull();

        assertThat(jedis.exists("framework")).isTrue();
        assertThat(jedis.del("framework")).isEqualTo(1);
        assertThat(jedis.exists("framework")).isFalse();
    }

    @Test
    void testIncrDecrCommands() {
        assertThat(jedis.incr("counter")).isEqualTo(1);
        assertThat(jedis.incr("counter")).isEqualTo(2);
        assertThat(jedis.incrBy("counter", 10)).isEqualTo(12);
        assertThat(jedis.decr("counter")).isEqualTo(11);
        assertThat(jedis.decrBy("counter", 5)).isEqualTo(6);
    }

    @Test
    void testListCommands() {
        assertThat(jedis.rpush("tasks", "task1", "task2")).isEqualTo(2);
        assertThat(jedis.lpush("tasks", "task0")).isEqualTo(3);
        assertThat(jedis.llen("tasks")).isEqualTo(3);

        List<String> range = jedis.lrange("tasks", 0, -1);
        assertThat(range).containsExactly("task0", "task1", "task2");

        assertThat(jedis.lpop("tasks")).isEqualTo("task0");
        assertThat(jedis.rpop("tasks")).isEqualTo("task2");
        assertThat(jedis.llen("tasks")).isEqualTo(1);
    }

    @Test
    void testSetCommands() {
        assertThat(jedis.sadd("languages", "java", "python", "rust")).isEqualTo(3);
        assertThat(jedis.sadd("languages", "java")).isEqualTo(0);
        assertThat(jedis.scard("languages")).isEqualTo(3);

        assertThat(jedis.sismember("languages", "java")).isTrue();
        assertThat(jedis.sismember("languages", "c++")).isFalse();

        Set<String> members = jedis.smembers("languages");
        assertThat(members).containsExactlyInAnyOrder("java", "python", "rust");

        assertThat(jedis.srem("languages", "python")).isEqualTo(1);
        assertThat(jedis.scard("languages")).isEqualTo(2);
    }

    @Test
    void testHashCommands() {
        assertThat(jedis.hset("user:100", Map.of("name", "Alice", "role", "dev"))).isEqualTo(2);
        assertThat(jedis.hget("user:100", "name")).isEqualTo("Alice");
        assertThat(jedis.hexists("user:100", "role")).isTrue();
        assertThat(jedis.hlen("user:100")).isEqualTo(2);

        Map<String, String> all = jedis.hgetAll("user:100");
        assertThat(all).containsEntry("name", "Alice").containsEntry("role", "dev");

        assertThat(jedis.hdel("user:100", "role")).isEqualTo(1);
        assertThat(jedis.hexists("user:100", "role")).isFalse();
    }

    @Test
    void testKeysDbsizeFlushdb() {
        jedis.set("k1", "v1");
        jedis.set("k2", "v2");
        jedis.set("other", "v3");

        assertThat(jedis.dbSize()).isEqualTo(3);

        Set<String> matched = jedis.keys("k*");
        assertThat(matched).containsExactlyInAnyOrder("k1", "k2");

        assertThat(jedis.type("k1")).isEqualTo("string");

        assertThat(jedis.flushDB()).isEqualTo("OK");
        assertThat(jedis.dbSize()).isEqualTo(0);
    }

    @Test
    void testExpireAndTtlCommands() throws InterruptedException {
        jedis.set("expiringKey", "quickVal");

        // Initial TTL: -1 (no expiry)
        assertThat(jedis.ttl("expiringKey")).isEqualTo(-1);

        // EXPIRE 10 seconds
        assertThat(jedis.expire("expiringKey", 10)).isEqualTo(1);
        assertThat(jedis.ttl("expiringKey")).isGreaterThan(0).isLessThanOrEqualTo(10);

        // PERSIST
        assertThat(jedis.persist("expiringKey")).isEqualTo(1);
        assertThat(jedis.ttl("expiringKey")).isEqualTo(-1);

        // PEXPIRE 100 milliseconds
        assertThat(jedis.pexpire("expiringKey", 100)).isEqualTo(1);
        assertThat(jedis.pttl("expiringKey")).isGreaterThan(0).isLessThanOrEqualTo(100);

        Thread.sleep(130);

        // Key should now be expired
        assertThat(jedis.get("expiringKey")).isNull();
        assertThat(jedis.ttl("expiringKey")).isEqualTo(-2);
    }

    @Test
    void testSaveAndRestartRecovery() throws Exception {
        java.nio.file.Path tempRdb = java.nio.file.Files.createTempFile("rexredis-test", ".rdb");
        tempRdb.toFile().deleteOnExit();

        ServerConfig pConfig = new ServerConfig(0);
        pConfig.setRdbFilename(tempRdb.toString());
        pConfig.setPersistenceEnabled(true);

        RexRedisServer pServer = new RexRedisServer(pConfig);
        Thread pThread = new Thread(pServer::start);
        pThread.start();
        pServer.awaitRunning();

        try (Jedis pClient = new Jedis("localhost", pServer.getBoundPort())) {
            pClient.set("persistentData", "savedValue");
            pClient.rpush("savedList", "item1", "item2");
            pClient.hset("savedHash", "k1", "v1");
            assertThat(pClient.save()).isEqualTo("OK");
        }

        pServer.stop();
        pThread.join(2000);

        // Boot a new server pointing to the same RDB file
        ServerConfig restartConfig = new ServerConfig(0);
        restartConfig.setRdbFilename(tempRdb.toString());
        restartConfig.setPersistenceEnabled(true);

        RexRedisServer restartedServer = new RexRedisServer(restartConfig);
        Thread restartThread = new Thread(restartedServer::start);
        restartThread.start();
        restartedServer.awaitRunning();

        try (Jedis restartClient = new Jedis("localhost", restartedServer.getBoundPort())) {
            assertThat(restartClient.get("persistentData")).isEqualTo("savedValue");
            assertThat(restartClient.lrange("savedList", 0, -1)).containsExactly("item1", "item2");
            assertThat(restartClient.hget("savedHash", "k1")).isEqualTo("v1");
        } finally {
            restartedServer.stop();
            restartThread.join(2000);
            java.nio.file.Files.deleteIfExists(tempRdb);
        }
    }

    @Test
    void testPubSubOverNetwork() throws Exception {
        java.util.concurrent.CountDownLatch subscribedLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch messageLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> receivedMessage = new java.util.concurrent.atomic.AtomicReference<>();

        redis.clients.jedis.JedisPubSub listener = new redis.clients.jedis.JedisPubSub() {
            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                subscribedLatch.countDown();
            }

            @Override
            public void onMessage(String channel, String message) {
                receivedMessage.set(message);
                messageLatch.countDown();
            }
        };

        // Start subscriber on a separate thread
        Thread subThread = new Thread(() -> {
            try (Jedis subClient = new Jedis("localhost", port)) {
                subClient.subscribe(listener, "notifications");
            } catch (Exception ignored) {
            }
        });
        subThread.start();

        // Wait until subscriber is ready
        assertThat(subscribedLatch.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        // Now publish using another client
        try (Jedis pubClient = new Jedis("localhost", port)) {
            long receivedCount = pubClient.publish("notifications", "Hello from RexRedis PubSub!");
            assertThat(receivedCount).isEqualTo(1);
        }

        // Wait for subscriber to receive the message
        assertThat(messageLatch.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(receivedMessage.get()).isEqualTo("Hello from RexRedis PubSub!");

        // Unsubscribe and stop
        listener.unsubscribe();
        subThread.join(2000);
    }
}
