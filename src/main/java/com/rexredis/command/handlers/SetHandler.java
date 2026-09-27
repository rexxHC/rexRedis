package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

/**
 * Handles SET key value [EX seconds] [PX milliseconds] [NX|XX]
 */
public class SetHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        if (cmd.argCount() < 2) {
            return RespObject.error("ERR wrong number of arguments for 'set' command");
        }

        String key = cmd.arg(0);
        String value = cmd.arg(1);

        Long expiryMs = null;
        boolean onlyIfNotExists = false;
        boolean onlyIfExists = false;

        // Parse optional arguments
        for (int i = 2; i < cmd.argCount(); i++) {
            String opt = cmd.arg(i).toUpperCase();
            switch (opt) {
                case "EX" -> {
                    if (i + 1 >= cmd.argCount()) {
                        return RespObject.error("ERR syntax error");
                    }
                    try {
                        long seconds = Long.parseLong(cmd.arg(++i));
                        if (seconds <= 0) {
                            return RespObject.error("ERR invalid expire time in 'set' command");
                        }
                        expiryMs = seconds * 1000;
                    } catch (NumberFormatException e) {
                        return RespObject.error("ERR value is not an integer or out of range");
                    }
                }
                case "PX" -> {
                    if (i + 1 >= cmd.argCount()) {
                        return RespObject.error("ERR syntax error");
                    }
                    try {
                        long millis = Long.parseLong(cmd.arg(++i));
                        if (millis <= 0) {
                            return RespObject.error("ERR invalid expire time in 'set' command");
                        }
                        expiryMs = millis;
                    } catch (NumberFormatException e) {
                        return RespObject.error("ERR value is not an integer or out of range");
                    }
                }
                case "NX" -> onlyIfNotExists = true;
                case "XX" -> onlyIfExists = true;
                default -> {
                    return RespObject.error("ERR syntax error");
                }
            }
        }

        if (onlyIfNotExists && onlyIfExists) {
            return RespObject.error("ERR syntax error");
        }

        boolean exists = store.exists(key);
        if (onlyIfNotExists && exists) {
            return RespObject.nullBulkString();
        }
        if (onlyIfExists && !exists) {
            return RespObject.nullBulkString();
        }

        store.set(key, RedisValue.string(value));
        if (expiryMs != null) {
            store.getExpiryManager().setExpiry(key, expiryMs);
        }

        return RespObject.ok();
    }
}
