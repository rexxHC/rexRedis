package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SetGetHandlerTest {

    private DataStore dataStore;
    private SetHandler setHandler;
    private GetHandler getHandler;
    private IncrHandler incrHandler;
    private DelHandler delHandler;
    private ExistsHandler existsHandler;

    @BeforeEach
    void setUp() {
        dataStore = new DataStore();
        setHandler = new SetHandler();
        getHandler = new GetHandler();
        incrHandler = new IncrHandler();
        delHandler = new DelHandler();
        existsHandler = new ExistsHandler();
    }

    @Test
    void testSetAndGet() {
        RespObject setRes = setHandler.handle(new Command("SET", List.of("mykey", "hello")), dataStore);
        assertThat(setRes).isEqualTo(RespObject.ok());

        RespObject getRes = getHandler.handle(new Command("GET", List.of("mykey")), dataStore);
        assertThat(getRes).isInstanceOf(RespObject.BulkString.class);
        assertThat(((RespObject.BulkString) getRes).value()).isEqualTo("hello");
    }

    @Test
    void testGetMissingKeyReturnsNullBulkString() {
        RespObject getRes = getHandler.handle(new Command("GET", List.of("nonexistent")), dataStore);
        assertThat(getRes).isEqualTo(RespObject.nullBulkString());
    }

    @Test
    void testSetNxOption() {
        // First SET NX succeeds
        RespObject res1 = setHandler.handle(new Command("SET", List.of("lock", "val1", "NX")), dataStore);
        assertThat(res1).isEqualTo(RespObject.ok());

        // Second SET NX returns null bulk string (nil)
        RespObject res2 = setHandler.handle(new Command("SET", List.of("lock", "val2", "NX")), dataStore);
        assertThat(res2).isEqualTo(RespObject.nullBulkString());

        // Value remains val1
        RespObject getRes = getHandler.handle(new Command("GET", List.of("lock")), dataStore);
        assertThat(((RespObject.BulkString) getRes).value()).isEqualTo("val1");
    }

    @Test
    void testSetXxOption() {
        // SET XX on missing key returns nil
        RespObject res1 = setHandler.handle(new Command("SET", List.of("key", "val1", "XX")), dataStore);
        assertThat(res1).isEqualTo(RespObject.nullBulkString());

        // Create key
        setHandler.handle(new Command("SET", List.of("key", "val1")), dataStore);

        // SET XX now succeeds
        RespObject res2 = setHandler.handle(new Command("SET", List.of("key", "val2", "XX")), dataStore);
        assertThat(res2).isEqualTo(RespObject.ok());

        RespObject getRes = getHandler.handle(new Command("GET", List.of("key")), dataStore);
        assertThat(((RespObject.BulkString) getRes).value()).isEqualTo("val2");
    }

    @Test
    void testSetWithExAndPx() {
        setHandler.handle(new Command("SET", List.of("k1", "v1", "EX", "10")), dataStore);
        assertThat(dataStore.getExpiryManager().hasExpiry("k1")).isTrue();
        assertThat(dataStore.getExpiryManager().ttlMillis("k1")).isGreaterThan(0);

        setHandler.handle(new Command("SET", List.of("k2", "v2", "PX", "5000")), dataStore);
        assertThat(dataStore.getExpiryManager().hasExpiry("k2")).isTrue();
        assertThat(dataStore.getExpiryManager().ttlMillis("k2")).isGreaterThan(0);
    }

    @Test
    void testIncrAndDecr() {
        // INCR on non-existing key initializes to 1
        RespObject incr1 = incrHandler.handle(new Command("INCR", List.of("counter")), dataStore);
        assertThat(incr1).isEqualTo(RespObject.integer(1));

        // INCR again -> 2
        RespObject incr2 = incrHandler.handle(new Command("INCR", List.of("counter")), dataStore);
        assertThat(incr2).isEqualTo(RespObject.integer(2));

        // INCRBY 10 -> 12
        RespObject incrBy = incrHandler.handle(new Command("INCRBY", List.of("counter", "10")), dataStore);
        assertThat(incrBy).isEqualTo(RespObject.integer(12));

        // DECR -> 11
        RespObject decr = incrHandler.handle(new Command("DECR", List.of("counter")), dataStore);
        assertThat(decr).isEqualTo(RespObject.integer(11));

        // DECRBY 5 -> 6
        RespObject decrBy = incrHandler.handle(new Command("DECRBY", List.of("counter", "5")), dataStore);
        assertThat(decrBy).isEqualTo(RespObject.integer(6));
    }

    @Test
    void testIncrNonIntegerError() {
        setHandler.handle(new Command("SET", List.of("strKey", "notANumber")), dataStore);
        RespObject res = incrHandler.handle(new Command("INCR", List.of("strKey")), dataStore);
        assertThat(res).isInstanceOf(RespObject.Error.class);
        assertThat(((RespObject.Error) res).message()).contains("value is not an integer");
    }

    @Test
    void testDelAndExists() {
        setHandler.handle(new Command("SET", List.of("a", "1")), dataStore);
        setHandler.handle(new Command("SET", List.of("b", "2")), dataStore);

        RespObject existsRes = existsHandler.handle(new Command("EXISTS", List.of("a", "b", "c")), dataStore);
        assertThat(existsRes).isEqualTo(RespObject.integer(2));

        RespObject delRes = delHandler.handle(new Command("DEL", List.of("a", "c")), dataStore);
        assertThat(delRes).isEqualTo(RespObject.integer(1));

        assertThat(dataStore.exists("a")).isFalse();
        assertThat(dataStore.exists("b")).isTrue();
    }

    @Test
    void testWrongTypeOnGet() {
        dataStore.set("listKey", RedisValue.list());
        RespObject res = getHandler.handle(new Command("GET", List.of("listKey")), dataStore);
        assertThat(res).isEqualTo(RespObject.wrongType());
    }

    @Test
    void testIncrPreservesTtl() {
        setHandler.handle(new Command("SET", List.of("ttlKey", "10")), dataStore);
        dataStore.getExpiryManager().setExpiry("ttlKey", 100_000);

        RespObject res = incrHandler.handle(new Command("INCR", List.of("ttlKey")), dataStore);
        assertThat(res).isEqualTo(RespObject.integer(11));

        assertThat(dataStore.getExpiryManager().hasExpiry("ttlKey")).isTrue();
        assertThat(dataStore.getExpiryManager().ttlMillis("ttlKey")).isGreaterThan(0);
    }
}
