package com.rexredis.pubsub;

import com.rexredis.command.Command;
import com.rexredis.command.handlers.CommandHandler;
import com.rexredis.protocol.RespObject;
import com.rexredis.server.ClientHandler;
import com.rexredis.store.DataStore;

import java.util.List;
import java.util.Map;

/**
 * Handles SUBSCRIBE channel [channel ...] and UNSUBSCRIBE [channel ...] commands.
 */
public class SubscribeHandler implements CommandHandler {

    private final PubSubManager pubSubManager;

    public SubscribeHandler(PubSubManager pubSubManager) {
        this.pubSubManager = pubSubManager;
    }

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        return RespObject.error("ERR subscribe/unsubscribe requires active client connection");
    }

    @Override
    public RespObject handle(Command cmd, DataStore store, ClientHandler client) {
        if (client == null) {
            return handle(cmd, store);
        }

        String name = cmd.name().toUpperCase();
        if ("SUBSCRIBE".equals(name)) {
            return handleSubscribe(cmd, client);
        } else if ("UNSUBSCRIBE".equals(name)) {
            return handleUnsubscribe(cmd, client);
        }

        return RespObject.error("ERR unknown command '" + cmd.name() + "'");
    }

    private RespObject handleSubscribe(Command cmd, ClientHandler client) {
        if (cmd.argCount() < 1) {
            return RespObject.error("ERR wrong number of arguments for 'subscribe' command");
        }

        for (int i = 0; i < cmd.argCount(); i++) {
            String channel = cmd.arg(i);
            int count = pubSubManager.subscribe(channel, client);

            RespObject response = RespObject.array(List.of(
                    RespObject.bulkString("subscribe"),
                    RespObject.bulkString(channel),
                    RespObject.integer(count)
            ));
            client.sendResponse(response);
        }

        return null; // All responses have been directly dispatched
    }

    private RespObject handleUnsubscribe(Command cmd, ClientHandler client) {
        if (cmd.argCount() == 0) {
            List<Map.Entry<String, Integer>> unsubs = pubSubManager.unsubscribeAll(client);
            if (unsubs.isEmpty()) {
                RespObject response = RespObject.array(List.of(
                        RespObject.bulkString("unsubscribe"),
                        RespObject.nullBulkString(),
                        RespObject.integer(0)
                ));
                client.sendResponse(response);
            } else {
                for (Map.Entry<String, Integer> entry : unsubs) {
                    RespObject response = RespObject.array(List.of(
                            RespObject.bulkString("unsubscribe"),
                            RespObject.bulkString(entry.getKey()),
                            RespObject.integer(entry.getValue())
                    ));
                    client.sendResponse(response);
                }
            }
        } else {
            for (int i = 0; i < cmd.argCount(); i++) {
                String channel = cmd.arg(i);
                int remaining = pubSubManager.unsubscribe(channel, client);

                RespObject response = RespObject.array(List.of(
                        RespObject.bulkString("unsubscribe"),
                        RespObject.bulkString(channel),
                        RespObject.integer(remaining)
                ));
                client.sendResponse(response);
            }
        }

        return null; // All responses have been directly dispatched
    }
}
