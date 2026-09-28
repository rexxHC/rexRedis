package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Handles List operations: LPUSH, RPUSH, LPOP, RPOP, LLEN, LRANGE.
 */
public class LpushHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        String name = cmd.name().toUpperCase();
        return switch (name) {
            case "LPUSH" -> handlePush(cmd, store, true);
            case "RPUSH" -> handlePush(cmd, store, false);
            case "LPOP" -> handlePop(cmd, store, true);
            case "RPOP" -> handlePop(cmd, store, false);
            case "LLEN" -> handleLlen(cmd, store);
            case "LRANGE" -> handleLrange(cmd, store);
            default -> RespObject.error("ERR unknown command '" + cmd.name() + "'");
        };
    }

    private RespObject handlePush(Command cmd, DataStore store, boolean prepend) {
        if (cmd.argCount() < 2) {
            return RespObject.error("ERR wrong number of arguments for '" + cmd.name().toLowerCase() + "' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        LinkedList<String> list;
        if (val == null) {
            val = RedisValue.list();
            store.set(key, val);
            list = val.asList();
        } else {
            if (val.getType() != RedisValue.Type.LIST) {
                return RespObject.wrongType();
            }
            list = val.asList();
        }

        for (int i = 1; i < cmd.argCount(); i++) {
            if (prepend) {
                list.addFirst(cmd.arg(i));
            } else {
                list.addLast(cmd.arg(i));
            }
        }

        return RespObject.integer(list.size());
    }

    private RespObject handlePop(Command cmd, DataStore store, boolean fromHead) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for '" + cmd.name().toLowerCase() + "' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.nullBulkString();
        }

        if (val.getType() != RedisValue.Type.LIST) {
            return RespObject.wrongType();
        }

        LinkedList<String> list = val.asList();
        if (list.isEmpty()) {
            return RespObject.nullBulkString();
        }

        String item = fromHead ? list.removeFirst() : list.removeLast();
        if (list.isEmpty()) {
            store.delete(key);
        }

        return RespObject.bulkString(item);
    }

    private RespObject handleLlen(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'llen' command");
        }

        String key = cmd.arg(0);
        RedisValue val = store.get(key);

        if (val == null) {
            return RespObject.integer(0);
        }

        if (val.getType() != RedisValue.Type.LIST) {
            return RespObject.wrongType();
        }

        return RespObject.integer(val.asList().size());
    }

    private RespObject handleLrange(Command cmd, DataStore store) {
        if (cmd.argCount() != 3) {
            return RespObject.error("ERR wrong number of arguments for 'lrange' command");
        }

        String key = cmd.arg(0);
        int start;
        int stop;
        try {
            start = Integer.parseInt(cmd.arg(1));
            stop = Integer.parseInt(cmd.arg(2));
        } catch (NumberFormatException e) {
            return RespObject.error("ERR value is not an integer or out of range");
        }

        RedisValue val = store.get(key);
        if (val == null) {
            return RespObject.array(List.of());
        }

        if (val.getType() != RedisValue.Type.LIST) {
            return RespObject.wrongType();
        }

        LinkedList<String> list = val.asList();
        int size = list.size();
        if (size == 0) {
            return RespObject.array(List.of());
        }

        // Adjust negative indexes
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;

        if (start < 0) start = 0;
        if (start >= size || start > stop) {
            return RespObject.array(List.of());
        }

        if (stop >= size) {
            stop = size - 1;
        }

        List<RespObject> result = new ArrayList<>(stop - start + 1);
        ListIterator<String> it = list.listIterator(start);
        for (int i = start; i <= stop && it.hasNext(); i++) {
            result.add(RespObject.bulkString(it.next()));
        }

        return RespObject.array(result);
    }
}
