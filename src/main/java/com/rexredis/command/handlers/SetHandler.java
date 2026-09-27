package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** SET key value [EX seconds] [PX ms] [NX|XX] */
public class SetHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Implement SET with EX/PX/NX/XX options
        return RespObject.ok();
    }
}
