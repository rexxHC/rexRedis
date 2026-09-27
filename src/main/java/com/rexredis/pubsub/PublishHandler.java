package com.rexredis.pubsub;

import com.rexredis.command.Command;
import com.rexredis.command.handlers.CommandHandler;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/**
 * Handles PUBLISH channel message command.
 */
public class PublishHandler implements CommandHandler {

    private final PubSubManager pubSubManager;

    public PublishHandler(PubSubManager pubSubManager) {
        this.pubSubManager = pubSubManager;
    }

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        if (cmd.argCount() != 2) {
            return RespObject.error("ERR wrong number of arguments for 'publish' command");
        }

        String channel = cmd.arg(0);
        String message = cmd.arg(1);

        int receivers = pubSubManager.publish(channel, message);
        return RespObject.integer(receivers);
    }
}
