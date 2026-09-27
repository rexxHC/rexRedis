package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;

/**
 * Handles EXPIRE, PEXPIRE, EXPIREAT, PEXPIREAT, TTL, PTTL, and PERSIST commands.
 */
public class ExpireHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        String name = cmd.name().toUpperCase();
        return switch (name) {
            case "EXPIRE" -> handleExpire(cmd, store, 1000L);
            case "PEXPIRE" -> handleExpire(cmd, store, 1L);
            case "EXPIREAT" -> handleExpireAt(cmd, store, 1000L);
            case "PEXPIREAT" -> handleExpireAt(cmd, store, 1L);
            case "TTL" -> handleTtl(cmd, store, false);
            case "PTTL" -> handleTtl(cmd, store, true);
            case "PERSIST" -> handlePersist(cmd, store);
            default -> RespObject.error("ERR unknown command '" + cmd.name() + "'");
        };
    }

    private RespObject handleExpire(Command cmd, DataStore store, long multiplier) {
        if (cmd.argCount() != 2) {
            return RespObject.error("ERR wrong number of arguments for '" + cmd.name().toLowerCase() + "' command");
        }

        String key = cmd.arg(0);
        long ttl;
        try {
            ttl = Long.parseLong(cmd.arg(1));
        } catch (NumberFormatException e) {
            return RespObject.error("ERR value is not an integer or out of range");
        }

        if (!store.exists(key)) {
            return RespObject.integer(0);
        }

        long ttlMillis = ttl * multiplier;
        if (ttlMillis <= 0) {
            store.delete(key);
            return RespObject.integer(1);
        }

        store.getExpiryManager().setExpiry(key, ttlMillis);
        return RespObject.integer(1);
    }

    private RespObject handleExpireAt(Command cmd, DataStore store, long multiplier) {
        if (cmd.argCount() != 2) {
            return RespObject.error("ERR wrong number of arguments for '" + cmd.name().toLowerCase() + "' command");
        }

        String key = cmd.arg(0);
        long timestamp;
        try {
            timestamp = Long.parseLong(cmd.arg(1));
        } catch (NumberFormatException e) {
            return RespObject.error("ERR value is not an integer or out of range");
        }

        if (!store.exists(key)) {
            return RespObject.integer(0);
        }

        long timestampMillis = timestamp * multiplier;
        if (timestampMillis <= System.currentTimeMillis()) {
            store.delete(key);
            return RespObject.integer(1);
        }

        store.getExpiryManager().setAbsoluteExpiry(key, timestampMillis);
        return RespObject.integer(1);
    }

    private RespObject handleTtl(Command cmd, DataStore store, boolean inMillis) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for '" + cmd.name().toLowerCase() + "' command");
        }

        String key = cmd.arg(0);
        long ttl = inMillis
                ? store.getExpiryManager().ttlMillis(key, store)
                : store.getExpiryManager().ttlSeconds(key, store);

        return RespObject.integer(ttl);
    }

    private RespObject handlePersist(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'persist' command");
        }

        String key = cmd.arg(0);
        if (!store.exists(key)) {
            return RespObject.integer(0);
        }

        boolean removed = store.getExpiryManager().removeExpiry(key);
        return RespObject.integer(removed ? 1 : 0);
    }
}
