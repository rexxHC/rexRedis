package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
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
}
