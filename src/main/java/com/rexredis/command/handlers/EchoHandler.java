package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** ECHO &lt;message&gt; */
public class EchoHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        if (cmd.argCount() == 0) {
            return RespObject.error("ERR wrong number of arguments for 'echo' command");
        }
        return RespObject.bulkString(cmd.arg(0));
    }
}
