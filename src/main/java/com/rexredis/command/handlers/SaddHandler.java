package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** SADD / SMEMBERS / SISMEMBER / SCARD / SREM */
public class SaddHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Implement set operations
        return RespObject.integer(0);
    }
}
