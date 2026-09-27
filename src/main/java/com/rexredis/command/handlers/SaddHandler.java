package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles Set operations: SADD, SREM, SMEMBERS, SISMEMBER, SCARD.
 */
public class SaddHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        String name = cmd.name().toUpperCase();
        return switch (name) {
            case "SADD" -> handleSadd(cmd, store);
            case "SREM" -> handleSrem(cmd, store);
            case "SMEMBERS" -> handleSmembers(cmd, store);
            case "SISMEMBER" -> handleSismember(cmd, store);
            case "SCARD" -> handleScard(cmd, store);
            default -> RespObject.error("ERR unknown command '" + cmd.name() + "'");
        };
    }

    private RespObject handleSadd(Command cmd, DataStore store) {
        if (cmd.argCount() < 2) {
            return RespObject.error("ERR wrong number of arguments for 'sadd' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        Set<String> set;
        if (val == null) {
            val = RedisValue.set();
            store.set(key, val);
            set = val.asSet();
        } else {
            if (val.getType() != RedisValue.Type.SET) {
                return RespObject.wrongType();
            }
            set = val.asSet();
        }

        long added = 0;
        for (int i = 1; i < cmd.argCount(); i++) {
            if (set.add(cmd.arg(i))) {
                added++;
            }
        }

        return RespObject.integer(added);
    }

    private RespObject handleSrem(Command cmd, DataStore store) {
        if (cmd.argCount() < 2) {
            return RespObject.error("ERR wrong number of arguments for 'srem' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.integer(0);
        }

        if (val.getType() != RedisValue.Type.SET) {
            return RespObject.wrongType();
        }

        Set<String> set = val.asSet();
        long removed = 0;
        for (int i = 1; i < cmd.argCount(); i++) {
            if (set.remove(cmd.arg(i))) {
                removed++;
            }
        }

        if (set.isEmpty()) {
            store.delete(key);
        }

        return RespObject.integer(removed);
    }

    private RespObject handleSmembers(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'smembers' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.array(List.of());
        }

        if (val.getType() != RedisValue.Type.SET) {
            return RespObject.wrongType();
        }

        List<RespObject> members = new ArrayList<>();
        for (String member : val.asSet()) {
            members.add(RespObject.bulkString(member));
        }

        return RespObject.array(members);
    }

    private RespObject handleSismember(Command cmd, DataStore store) {
        if (cmd.argCount() != 2) {
            return RespObject.error("ERR wrong number of arguments for 'sismember' command");
        }

        String key = cmd.arg(0);
        String member = cmd.arg(1);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.integer(0);
        }

        if (val.getType() != RedisValue.Type.SET) {
            return RespObject.wrongType();
        }

        return RespObject.integer(val.asSet().contains(member) ? 1 : 0);
    }

    private RespObject handleScard(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'scard' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.integer(0);
        }

        if (val.getType() != RedisValue.Type.SET) {
            return RespObject.wrongType();
        }

        return RespObject.integer(val.asSet().size());
    }
}
