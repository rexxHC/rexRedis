package com.rexredis.command;

import com.rexredis.command.handlers.*;
import com.rexredis.config.ServerConfig;
import com.rexredis.protocol.RespObject;
import com.rexredis.pubsub.PublishHandler;
import com.rexredis.pubsub.PubSubManager;
import com.rexredis.pubsub.SubscribeHandler;
import com.rexredis.server.ClientHandler;
import com.rexredis.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps command names to their handlers and dispatches incoming commands.
 */
public class CommandRouter {

    private static final Logger logger = LoggerFactory.getLogger(CommandRouter.class);
    private final Map<String, CommandHandler> handlers = new HashMap<>();
    private final DataStore dataStore;
    private final ServerConfig config;
    private final PubSubManager pubSubManager;

    public CommandRouter(DataStore dataStore) {
        this(dataStore, new ServerConfig());
    }

    public CommandRouter(DataStore dataStore, ServerConfig config) {
        this.dataStore = dataStore;
        this.config = config;
        this.pubSubManager = new PubSubManager();
        registerDefaults();
    }

    private void registerDefaults() {
        register("PING", new PingHandler());
        register("ECHO", new EchoHandler());

        // Strings
        register("SET", new SetHandler());
        register("GET", new GetHandler());

        IncrHandler incrHandler = new IncrHandler();
        register("INCR", incrHandler);
        register("DECR", incrHandler);
        register("INCRBY", incrHandler);
        register("DECRBY", incrHandler);

        // Keys
        register("DEL", new DelHandler());
        register("EXISTS", new ExistsHandler());

        KeysHandler keysHandler = new KeysHandler();
        register("KEYS", keysHandler);
        register("TYPE", keysHandler);
        register("DBSIZE", keysHandler);
        register("FLUSHDB", keysHandler);

        // Lists
        LpushHandler listHandler = new LpushHandler();
        register("LPUSH", listHandler);
        register("RPUSH", listHandler);
        register("LPOP", listHandler);
        register("RPOP", listHandler);
        register("LLEN", listHandler);
        register("LRANGE", listHandler);

        // Sets
        SaddHandler setHandler = new SaddHandler();
        register("SADD", setHandler);
        register("SREM", setHandler);
        register("SMEMBERS", setHandler);
        register("SISMEMBER", setHandler);
        register("SCARD", setHandler);

        // Hashes
        HsetHandler hashHandler = new HsetHandler();
        register("HSET", hashHandler);
        register("HGET", hashHandler);
        register("HGETALL", hashHandler);
        register("HDEL", hashHandler);
        register("HEXISTS", hashHandler);
        register("HLEN", hashHandler);

        // Expiry & TTL
        ExpireHandler expireHandler = new ExpireHandler();
        register("EXPIRE", expireHandler);
        register("PEXPIRE", expireHandler);
        register("EXPIREAT", expireHandler);
        register("PEXPIREAT", expireHandler);
        register("TTL", expireHandler);
        register("PTTL", expireHandler);
        register("PERSIST", expireHandler);

        // Pub/Sub
        SubscribeHandler subscribeHandler = new SubscribeHandler(pubSubManager);
        register("SUBSCRIBE", subscribeHandler);
        register("UNSUBSCRIBE", subscribeHandler);
        register("PUBLISH", new PublishHandler(pubSubManager));

        // System & other
        SaveHandler saveHandler = new SaveHandler(config);
        register("SAVE", saveHandler);
        register("BGSAVE", saveHandler);
        register("INFO", new InfoHandler());
    }

    public void register(String name, CommandHandler handler) {
        handlers.put(name.toUpperCase(), handler);
    }

    /**
     * Dispatches a command with client context.
     *
     * @param cmd    the parsed command
     * @param client the client connection handler
     * @return the RESP response, or null if response was already dispatched
     */
    public RespObject dispatch(Command cmd, ClientHandler client) {
        String cmdName = cmd.name().toUpperCase();

        if (client != null && pubSubManager.isSubscribed(client)) {
            boolean allowed = switch (cmdName) {
                case "SUBSCRIBE", "UNSUBSCRIBE", "PSUBSCRIBE", "PUNSUBSCRIBE", "PING", "QUIT", "RESET" -> true;
                default -> false;
            };
            if (!allowed) {
                return RespObject.error("ERR only (P)SUBSCRIBE / (P)UNSUBSCRIBE / PING / QUIT / RESET are allowed in this context");
            }
        }

        CommandHandler handler = handlers.get(cmdName);
        if (handler == null) {
            logger.warn("Unknown command: {}", cmd.name());
            return RespObject.error("ERR unknown command '" + cmd.name() + "'");
        }
        return handler.handle(cmd, dataStore, client);
    }

    /**
     * Dispatches a command without client context.
     *
     * @param cmd the parsed command
     * @return the RESP response
     */
    public RespObject dispatch(Command cmd) {
        return dispatch(cmd, null);
    }

    public PubSubManager getPubSubManager() {
        return pubSubManager;
    }
}
