package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/**
 * Handles EXISTS key [key ...]
 */
public class ExistsHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        if (cmd.argCount() < 1) {
            return RespObject.error("ERR wrong number of arguments for 'exists' command");
        }

        long count = 0;
        for (int i = 0; i < cmd.argCount(); i++) {
            if (store.exists(cmd.arg(i))) {
                count++;
            }
        }

        return RespObject.integer(count);
    }
}
