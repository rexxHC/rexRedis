package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataStructuresTest {

    private DataStore dataStore;
    private LpushHandler listHandler;
    private SaddHandler setHandler;
    private HsetHandler hashHandler;
    private KeysHandler keysHandler;

    @BeforeEach
    void setUp() {
        dataStore = new DataStore();
        listHandler = new LpushHandler();
        setHandler = new SaddHandler();
        hashHandler = new HsetHandler();
        keysHandler = new KeysHandler();
    }

    @Test
    void testListOperations() {
        // RPUSH mylist a b
        RespObject rpush = listHandler.handle(new Command("RPUSH", List.of("mylist", "a", "b")), dataStore);
        assertThat(rpush).isEqualTo(RespObject.integer(2));

        // LPUSH mylist head
        RespObject lpush = listHandler.handle(new Command("LPUSH", List.of("mylist", "head")), dataStore);
        assertThat(lpush).isEqualTo(RespObject.integer(3));

        // LLEN
        RespObject llen = listHandler.handle(new Command("LLEN", List.of("mylist")), dataStore);
        assertThat(llen).isEqualTo(RespObject.integer(3));

        // LRANGE 0 -1
        RespObject lrange = listHandler.handle(new Command("LRANGE", List.of("mylist", "0", "-1")), dataStore);
        assertThat(lrange).isInstanceOf(RespObject.ArrayResp.class);
        List<RespObject> elements = ((RespObject.ArrayResp) lrange).elements();
        assertThat(elements).hasSize(3);
        assertThat(((RespObject.BulkString) elements.get(0)).value()).isEqualTo("head");
        assertThat(((RespObject.BulkString) elements.get(1)).value()).isEqualTo("a");
        assertThat(((RespObject.BulkString) elements.get(2)).value()).isEqualTo("b");

        // LPOP
        RespObject lpop = listHandler.handle(new Command("LPOP", List.of("mylist")), dataStore);
        assertThat(((RespObject.BulkString) lpop).value()).isEqualTo("head");

        // RPOP
        RespObject rpop = listHandler.handle(new Command("RPOP", List.of("mylist")), dataStore);
        assertThat(((RespObject.BulkString) rpop).value()).isEqualTo("b");
    }

    @Test
    void testSetOperations() {
        // SADD myset a b c
        RespObject sadd = setHandler.handle(new Command("SADD", List.of("myset", "a", "b", "c")), dataStore);
        assertThat(sadd).isEqualTo(RespObject.integer(3));

        // SADD duplicate
        RespObject saddDup = setHandler.handle(new Command("SADD", List.of("myset", "b", "d")), dataStore);
        assertThat(saddDup).isEqualTo(RespObject.integer(1)); // only 'd' was new

        // SCARD
        RespObject scard = setHandler.handle(new Command("SCARD", List.of("myset")), dataStore);
        assertThat(scard).isEqualTo(RespObject.integer(4));

        // SISMEMBER
        RespObject sismemberTrue = setHandler.handle(new Command("SISMEMBER", List.of("myset", "a")), dataStore);
        assertThat(sismemberTrue).isEqualTo(RespObject.integer(1));
        RespObject sismemberFalse = setHandler.handle(new Command("SISMEMBER", List.of("myset", "z")), dataStore);
        assertThat(sismemberFalse).isEqualTo(RespObject.integer(0));

        // SREM
        RespObject srem = setHandler.handle(new Command("SREM", List.of("myset", "a", "nonexistent")), dataStore);
        assertThat(srem).isEqualTo(RespObject.integer(1));

        // SMEMBERS
        RespObject smembers = setHandler.handle(new Command("SMEMBERS", List.of("myset")), dataStore);
        assertThat(smembers).isInstanceOf(RespObject.ArrayResp.class);
        assertThat(((RespObject.ArrayResp) smembers).elements()).hasSize(3);
    }

    @Test
    void testHashOperations() {
        // HSET user:1 name Rex role Admin
        RespObject hset = hashHandler.handle(new Command("HSET", List.of("user:1", "name", "Rex", "role", "Admin")), dataStore);
        assertThat(hset).isEqualTo(RespObject.integer(2));

        // HGET
        RespObject hget = hashHandler.handle(new Command("HGET", List.of("user:1", "name")), dataStore);
        assertThat(((RespObject.BulkString) hget).value()).isEqualTo("Rex");

        // HEXISTS
        RespObject hexists = hashHandler.handle(new Command("HEXISTS", List.of("user:1", "role")), dataStore);
        assertThat(hexists).isEqualTo(RespObject.integer(1));

        // HLEN
        RespObject hlen = hashHandler.handle(new Command("HLEN", List.of("user:1")), dataStore);
        assertThat(hlen).isEqualTo(RespObject.integer(2));

        // HGETALL
        RespObject hgetall = hashHandler.handle(new Command("HGETALL", List.of("user:1")), dataStore);
        assertThat(hgetall).isInstanceOf(RespObject.ArrayResp.class);
        assertThat(((RespObject.ArrayResp) hgetall).elements()).hasSize(4); // 2 pairs

        // HDEL
        RespObject hdel = hashHandler.handle(new Command("HDEL", List.of("user:1", "role")), dataStore);
        assertThat(hdel).isEqualTo(RespObject.integer(1));
        assertThat(hashHandler.handle(new Command("HEXISTS", List.of("user:1", "role")), dataStore))
                .isEqualTo(RespObject.integer(0));
    }

    @Test
    void testKeysAndType() {
        dataStore.set("foo", com.rexredis.store.RedisValue.string("bar"));
        listHandler.handle(new Command("RPUSH", List.of("fruits", "apple")), dataStore);
        setHandler.handle(new Command("SADD", List.of("tags", "java")), dataStore);
        hashHandler.handle(new Command("HSET", List.of("meta", "k", "v")), dataStore);

        // TYPE
        assertThat(keysHandler.handle(new Command("TYPE", List.of("foo")), dataStore))
                .isEqualTo(new RespObject.SimpleString("string"));
        assertThat(keysHandler.handle(new Command("TYPE", List.of("fruits")), dataStore))
                .isEqualTo(new RespObject.SimpleString("list"));
        assertThat(keysHandler.handle(new Command("TYPE", List.of("tags")), dataStore))
                .isEqualTo(new RespObject.SimpleString("set"));
        assertThat(keysHandler.handle(new Command("TYPE", List.of("meta")), dataStore))
                .isEqualTo(new RespObject.SimpleString("hash"));
        assertThat(keysHandler.handle(new Command("TYPE", List.of("missing")), dataStore))
                .isEqualTo(new RespObject.SimpleString("none"));

        // KEYS
        RespObject allKeys = keysHandler.handle(new Command("KEYS", List.of("*")), dataStore);
        assertThat(allKeys).isInstanceOf(RespObject.ArrayResp.class);
        assertThat(((RespObject.ArrayResp) allKeys).elements()).hasSize(4);

        // DBSIZE
        assertThat(keysHandler.handle(new Command("DBSIZE", List.of()), dataStore))
                .isEqualTo(RespObject.integer(4));

        // FLUSHDB
        assertThat(keysHandler.handle(new Command("FLUSHDB", List.of()), dataStore))
                .isEqualTo(RespObject.ok());
        assertThat(dataStore.size()).isEqualTo(0);
    }

    @Test
    void testKeysGlobMatching() {
        dataStore.set("a?x", com.rexredis.store.RedisValue.string("1"));
        dataStore.set("abx", com.rexredis.store.RedisValue.string("2"));
        dataStore.set("a\\x", com.rexredis.store.RedisValue.string("3"));

        RespObject res = keysHandler.handle(new Command("KEYS", List.of("*\\?x")), dataStore);
        assertThat(res).isInstanceOf(RespObject.ArrayResp.class);
        List<RespObject> elements = ((RespObject.ArrayResp) res).elements();
        assertThat(elements).hasSize(1);
        assertThat(((RespObject.BulkString) elements.get(0)).value()).isEqualTo("a?x");
    }
}
