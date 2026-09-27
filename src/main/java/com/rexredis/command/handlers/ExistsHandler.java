package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** EXISTS key [key ...] */
public class ExistsHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Check existence, return count
        return RespObject.integer(0);
    }
}
