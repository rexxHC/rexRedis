package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** KEYS pattern */
public class KeysHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Implement glob pattern matching over all keys
        return RespObject.array(java.util.List.of());
    }
}
