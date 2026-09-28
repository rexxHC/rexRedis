package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

/**
 * Handles INCR, DECR, INCRBY, and DECRBY commands.
 */
public class IncrHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        String cmdName = cmd.name().toUpperCase();
        long step;

        switch (cmdName) {
            case "INCR" -> {
                if (cmd.argCount() != 1) {
                    return RespObject.error("ERR wrong number of arguments for 'incr' command");
                }
                step = 1L;
            }
            case "DECR" -> {
                if (cmd.argCount() != 1) {
                    return RespObject.error("ERR wrong number of arguments for 'decr' command");
                }
                step = -1L;
            }
            case "INCRBY" -> {
                if (cmd.argCount() != 2) {
                    return RespObject.error("ERR wrong number of arguments for 'incrby' command");
                }
                try {
                    step = Long.parseLong(cmd.arg(1));
                } catch (NumberFormatException e) {
                    return RespObject.error("ERR value is not an integer or out of range");
                }
            }
            case "DECRBY" -> {
                if (cmd.argCount() != 2) {
                    return RespObject.error("ERR wrong number of arguments for 'decrby' command");
                }
                try {
                    long parsed = Long.parseLong(cmd.arg(1));
                    step = Math.negateExact(parsed);
                } catch (NumberFormatException e) {
                    return RespObject.error("ERR value is not an integer or out of range");
                } catch (ArithmeticException e) {
                    return RespObject.error("ERR increment or decrement would overflow");
                }
            }
            default -> {
                return RespObject.error("ERR unknown command '" + cmd.name() + "'");
            }
        }

        String key = cmd.arg(0);
        RedisValue currentVal = store.get(key);

        long currentNum = 0;
        if (currentVal != null) {
            if (currentVal.getType() != RedisValue.Type.STRING) {
                return RespObject.wrongType();
            }
            try {
                currentNum = Long.parseLong(currentVal.asString());
            } catch (NumberFormatException e) {
                return RespObject.error("ERR value is not an integer or out of range");
            }
        }

        long result;
        try {
            result = Math.addExact(currentNum, step);
        } catch (ArithmeticException e) {
            return RespObject.error("ERR increment or decrement would overflow");
        }

        // Mutate in-place to preserve TTL when key already exists
        if (currentVal != null) {
            currentVal.setValue(Long.toString(result));
        } else {
            store.set(key, RedisValue.string(Long.toString(result)));
        }

        return RespObject.integer(result);
    }
}
