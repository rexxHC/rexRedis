package com.rexredis.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests.
 * Boots the RexRedis server, connects with Jedis client, and verifies commands.
 */
class RexRedisIntegrationTest {

    // TODO: Test cases:
    //  - Boot server on random port
    //  - Connect with Jedis
    //  - PING → PONG
    //  - SET/GET round-trip
    //  - DEL removes key
    //  - EXPIRE + TTL
    //  - List operations (LPUSH/RPUSH/LRANGE)
    //  - Set operations (SADD/SMEMBERS)
    //  - Hash operations (HSET/HGET/HGETALL)
    //  - Shutdown server

    @Test
    void placeholder() {
        assertThat(true).isTrue();
    }
}
