package com.rexredis.pubsub;

import com.rexredis.command.Command;
import com.rexredis.command.handlers.CommandHandler;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** PUBLISH channel message */
public class PublishHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Fan-out message to subscribers, return recipient count
        return RespObject.integer(0);
    }
}
