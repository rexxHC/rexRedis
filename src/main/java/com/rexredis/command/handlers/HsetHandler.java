package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** HSET / HGET / HGETALL / HDEL / HEXISTS / HLEN */
public class HsetHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Implement hash operations
        return RespObject.integer(0);
    }
}
