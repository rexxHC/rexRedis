package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/**
 * Handles DEL key [key ...]
 */
public class DelHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        if (cmd.argCount() < 1) {
            return RespObject.error("ERR wrong number of arguments for 'del' command");
        }

        long deletedCount = 0;
        for (int i = 0; i < cmd.argCount(); i++) {
            if (store.delete(cmd.arg(i))) {
                deletedCount++;
            }
        }

        return RespObject.integer(deletedCount);
    }
}
