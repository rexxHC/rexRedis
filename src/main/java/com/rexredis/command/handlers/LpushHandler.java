package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** LPUSH / RPUSH / LPOP / RPOP / LRANGE */
public class LpushHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Implement list operations
        return RespObject.integer(0);
    }
}
