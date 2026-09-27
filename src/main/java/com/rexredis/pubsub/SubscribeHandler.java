package com.rexredis.pubsub;

import com.rexredis.command.Command;
import com.rexredis.command.handlers.CommandHandler;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** SUBSCRIBE channel [channel ...] */
public class SubscribeHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Enter subscription mode for the client
        return RespObject.ok();
    }
}
