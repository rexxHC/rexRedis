package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** PING → PONG */
public class PingHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        if (cmd.argCount() > 0) {
            return RespObject.bulkString(cmd.arg(0));
        }
        return new RespObject.SimpleString("PONG");
    }
}
