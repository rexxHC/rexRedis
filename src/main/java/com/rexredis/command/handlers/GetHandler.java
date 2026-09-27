package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

/**
 * Handles GET key
 */
public class GetHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'get' command");
        }

        String key = cmd.arg(0);
        RedisValue value = store.get(key);

        if (value == null) {
            return RespObject.nullBulkString();
        }

        if (value.getType() != RedisValue.Type.STRING) {
            return RespObject.wrongType();
        }

        return RespObject.bulkString(value.asString());
    }
}
