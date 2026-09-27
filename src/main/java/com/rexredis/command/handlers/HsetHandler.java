package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles Hash operations: HSET, HGET, HGETALL, HDEL, HEXISTS, HLEN.
 */
public class HsetHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        String name = cmd.name().toUpperCase();
        return switch (name) {
            case "HSET" -> handleHset(cmd, store);
            case "HGET" -> handleHget(cmd, store);
            case "HGETALL" -> handleHgetall(cmd, store);
            case "HDEL" -> handleHdel(cmd, store);
            case "HEXISTS" -> handleHexists(cmd, store);
            case "HLEN" -> handleHlen(cmd, store);
            default -> RespObject.error("ERR unknown command '" + cmd.name() + "'");
        };
    }

    private RespObject handleHset(Command cmd, DataStore store) {
        // HSET key field value [field value ...]
        if (cmd.argCount() < 3 || (cmd.argCount() - 1) % 2 != 0) {
            return RespObject.error("ERR wrong number of arguments for 'hset' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        Map<String, String> map;
        if (val == null) {
            val = RedisValue.hash();
            store.set(key, val);
            map = val.asHash();
        } else {
            if (val.getType() != RedisValue.Type.HASH) {
                return RespObject.wrongType();
            }
            map = val.asHash();
        }

        long newlyAdded = 0;
        for (int i = 1; i < cmd.argCount(); i += 2) {
            String field = cmd.arg(i);
            String value = cmd.arg(i + 1);
            if (map.put(field, value) == null) {
                newlyAdded++;
            }
        }

        return RespObject.integer(newlyAdded);
    }

    private RespObject handleHget(Command cmd, DataStore store) {
        if (cmd.argCount() != 2) {
            return RespObject.error("ERR wrong number of arguments for 'hget' command");
        }

        String key = cmd.arg(0);
        String field = cmd.arg(1);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.nullBulkString();
        }

        if (val.getType() != RedisValue.Type.HASH) {
            return RespObject.wrongType();
        }

        String value = val.asHash().get(field);
        if (value == null) {
            return RespObject.nullBulkString();
        }

        return RespObject.bulkString(value);
    }

    private RespObject handleHgetall(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'hgetall' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.array(List.of());
        }

        if (val.getType() != RedisValue.Type.HASH) {
            return RespObject.wrongType();
        }

        Map<String, String> map = val.asHash();
        List<RespObject> result = new ArrayList<>(map.size() * 2);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.add(RespObject.bulkString(entry.getKey()));
            result.add(RespObject.bulkString(entry.getValue()));
        }

        return RespObject.array(result);
    }

    private RespObject handleHdel(Command cmd, DataStore store) {
        if (cmd.argCount() < 2) {
            return RespObject.error("ERR wrong number of arguments for 'hdel' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.integer(0);
        }

        if (val.getType() != RedisValue.Type.HASH) {
            return RespObject.wrongType();
        }

        Map<String, String> map = val.asHash();
        long deleted = 0;
        for (int i = 1; i < cmd.argCount(); i++) {
            if (map.remove(cmd.arg(i)) != null) {
                deleted++;
            }
        }

        if (map.isEmpty()) {
            store.delete(key);
        }

        return RespObject.integer(deleted);
    }

    private RespObject handleHexists(Command cmd, DataStore store) {
        if (cmd.argCount() != 2) {
            return RespObject.error("ERR wrong number of arguments for 'hexists' command");
        }

        String key = cmd.arg(0);
        String field = cmd.arg(1);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.integer(0);
        }

        if (val.getType() != RedisValue.Type.HASH) {
            return RespObject.wrongType();
        }

        return RespObject.integer(val.asHash().containsKey(field) ? 1 : 0);
    }

    private RespObject handleHlen(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'hlen' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.integer(0);
        }

        if (val.getType() != RedisValue.Type.HASH) {
            return RespObject.wrongType();
        }

        return RespObject.integer(val.asHash().size());
    }
}
