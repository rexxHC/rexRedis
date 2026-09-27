package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.server.ClientHandler;
import com.rexredis.store.DataStore;

/**
 * Interface that all command handlers must implement.
 */
public interface CommandHandler {

    /**
     * Executes the command against the data store.
     *
     * @param cmd   the parsed command with arguments
     * @param store the data store
     * @return the RESP response to send back to the client
     */
    RespObject handle(Command cmd, DataStore store);

    /**
     * Executes the command with client context (needed for Pub/Sub and connection-scoped state).
     *
     * @param cmd    the parsed command with arguments
     * @param store  the data store
     * @param client the client connection handler
     * @return the RESP response to send back to the client, or null if response was already dispatched
     */
    default RespObject handle(Command cmd, DataStore store, ClientHandler client) {
        return handle(cmd, store);
    }
}
