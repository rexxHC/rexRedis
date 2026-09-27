package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpireHandlerTest {

    private DataStore store;
    private ExpireHandler handler;

    @BeforeEach
    void setUp() {
        store = new DataStore();
        handler = new ExpireHandler();
    }

    @Test
    void testExpireAndTtl() {
        store.set("session", RedisValue.string("abc"));

        // TTL before setting expiry -> -1
        RespObject ttlBefore = handler.handle(new Command("TTL", List.of("session")), store);
        assertThat(ttlBefore).isEqualTo(RespObject.integer(-1));

        // EXPIRE session 60 -> 1
        RespObject expireRes = handler.handle(new Command("EXPIRE", List.of("session", "60")), store);
        assertThat(expireRes).isEqualTo(RespObject.integer(1));

        // TTL after setting expiry -> positive value <= 60
        RespObject ttlAfter = handler.handle(new Command("TTL", List.of("session")), store);
        assertThat(ttlAfter).isInstanceOf(RespObject.IntegerResp.class);
        assertThat(((RespObject.IntegerResp) ttlAfter).value()).isGreaterThan(0).isLessThanOrEqualTo(60);

        // PTTL -> positive value <= 60000
        RespObject pttl = handler.handle(new Command("PTTL", List.of("session")), store);
        assertThat(((RespObject.IntegerResp) pttl).value()).isGreaterThan(0).isLessThanOrEqualTo(60000);
    }

    @Test
    void testExpireNonExistentKeyReturnsZero() {
        RespObject res = handler.handle(new Command("EXPIRE", List.of("missing", "10")), store);
        assertThat(res).isEqualTo(RespObject.integer(0));

        RespObject ttl = handler.handle(new Command("TTL", List.of("missing")), store);
        assertThat(ttl).isEqualTo(RespObject.integer(-2));
    }

    @Test
    void testPexpire() {
        store.set("fast", RedisValue.string("val"));
        RespObject pexpireRes = handler.handle(new Command("PEXPIRE", List.of("fast", "5000")), store);
        assertThat(pexpireRes).isEqualTo(RespObject.integer(1));

        RespObject pttl = handler.handle(new Command("PTTL", List.of("fast")), store);
        assertThat(((RespObject.IntegerResp) pttl).value()).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    void testExpireAtAndPexpireAt() {
        store.set("epochKey", RedisValue.string("val"));

        long futureSec = (System.currentTimeMillis() / 1000) + 100;
        RespObject expireAtRes = handler.handle(new Command("EXPIREAT", List.of("epochKey", Long.toString(futureSec))), store);
        assertThat(expireAtRes).isEqualTo(RespObject.integer(1));

        long futureMs = System.currentTimeMillis() + 100_000;
        RespObject pexpireAtRes = handler.handle(new Command("PEXPIREAT", List.of("epochKey", Long.toString(futureMs))), store);
        assertThat(pexpireAtRes).isEqualTo(RespObject.integer(1));
    }

    @Test
    void testExpireWithZeroOrNegativeTtlDeletesKey() {
        store.set("deadKey", RedisValue.string("val"));
        RespObject res = handler.handle(new Command("EXPIRE", List.of("deadKey", "0")), store);
        assertThat(res).isEqualTo(RespObject.integer(1));
        assertThat(store.exists("deadKey")).isFalse();
    }

    @Test
    void testPersist() {
        store.set("k", RedisValue.string("v"));
        handler.handle(new Command("EXPIRE", List.of("k", "30")), store);

        // PERSIST on key with TTL -> 1
        RespObject persist1 = handler.handle(new Command("PERSIST", List.of("k")), store);
        assertThat(persist1).isEqualTo(RespObject.integer(1));

        // Key still exists, but TTL is -1
        assertThat(store.exists("k")).isTrue();
        RespObject ttl = handler.handle(new Command("TTL", List.of("k")), store);
        assertThat(ttl).isEqualTo(RespObject.integer(-1));

        // PERSIST again -> 0
        RespObject persist2 = handler.handle(new Command("PERSIST", List.of("k")), store);
        assertThat(persist2).isEqualTo(RespObject.integer(0));
    }
}
