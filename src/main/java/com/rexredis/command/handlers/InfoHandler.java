package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/** INFO — returns server information */
public class InfoHandler implements CommandHandler {
    @Override
    public RespObject handle(Command cmd, DataStore store) {
        // TODO: Return server info (version, uptime, connected clients, memory, keyspace)
        return RespObject.bulkString("# Server\r\nrex_redis_version:1.0.0\r\n");
    }
}
