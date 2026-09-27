package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** INCR / DECR key */
public class IncrHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Increment/decrement integer value, create with 0 if missing
        return RespObject.integer(0);
    }
}
