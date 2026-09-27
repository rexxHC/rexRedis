package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** EXPIRE key seconds / TTL key / PEXPIRE / PTTL */
public class ExpireHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Set/query key expiry
        return RespObject.integer(0);
    }
}
