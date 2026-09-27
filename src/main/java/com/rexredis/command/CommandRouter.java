package com.rexredis.command;

import com.rexredis.command.handlers.*;
import com.rexredis.protocol.RespObject;
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

    public CommandRouter(DataStore dataStore) {
        this.dataStore = dataStore;
        registerDefaults();
    }

    private void registerDefaults() {
        register("PING", new PingHandler());
        register("ECHO", new EchoHandler());
        register("SET", new SetHandler());
        register("GET", new GetHandler());
        register("DEL", new DelHandler());
        register("EXISTS", new ExistsHandler());
        register("INCR", new IncrHandler());
        register("LPUSH", new LpushHandler());
        register("SADD", new SaddHandler());
        register("HSET", new HsetHandler());
        register("EXPIRE", new ExpireHandler());
        register("KEYS", new KeysHandler());
        register("SAVE", new SaveHandler());
        register("INFO", new InfoHandler());
    }

    public void register(String name, CommandHandler handler) {
        handlers.put(name.toUpperCase(), handler);
    }

    /**
     * Dispatches a command to its handler.
     *
     * @param cmd the parsed command
     * @return the RESP response
     */
    public RespObject dispatch(Command cmd) {
        CommandHandler handler = handlers.get(cmd.name().toUpperCase());
        if (handler == null) {
            logger.warn("Unknown command: {}", cmd.name());
            return RespObject.error("ERR unknown command '" + cmd.name() + "'");
        }
        return handler.handle(cmd, dataStore);
    }
}
