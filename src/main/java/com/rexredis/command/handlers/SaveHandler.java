package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** SAVE — triggers an RDB snapshot */
public class SaveHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Trigger RdbSaver to snapshot the DataStore to disk
        return RespObject.ok();
    }
}
