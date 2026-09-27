package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

import java.lang.management.ManagementFactory;

/**
 * Handles INFO command.
 */
public class InfoHandler implements CommandHandler {

    private final long startTime = System.currentTimeMillis();

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long totalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        int keysCount = store.size();
        int expiresCount = store.getExpiryManager().getAllExpiries().size();

        String info = """
                # Server
                redis_version:7.0.0-rexredis
                rexredis_version:1.0.0
                os_name:%s
                process_id:%s
                uptime_in_seconds:%d
                
                # Memory
                used_memory:%d
                
                # Keyspace
                db0:keys=%d,expires=%d
                """.formatted(
                System.getProperty("os.name"),
                ManagementFactory.getRuntimeMXBean().getName().split("@")[0],
                uptimeSeconds,
                totalMemory,
                keysCount,
                expiresCount
        );

        return RespObject.bulkString(info);
    }
}
