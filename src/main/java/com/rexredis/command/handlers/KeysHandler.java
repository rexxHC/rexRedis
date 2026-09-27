package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles KEYS pattern, TYPE key, DBSIZE, and FLUSHDB commands.
 */
public class KeysHandler implements CommandHandler {

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        String cmdName = cmd.name().toUpperCase();
        return switch (cmdName) {
            case "KEYS" -> handleKeys(cmd, store);
            case "TYPE" -> handleType(cmd, store);
            case "DBSIZE" -> RespObject.integer(store.size());
            case "FLUSHDB" -> {
                store.clear();
                yield RespObject.ok();
            }
            default -> RespObject.error("ERR unknown command '" + cmd.name() + "'");
        };
    }

    private RespObject handleKeys(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'keys' command");
        }

        String patternStr = cmd.arg(0);
        Pattern regex = globToRegex(patternStr);

        List<RespObject> matchingKeys = new ArrayList<>();
        for (String key : store.keys()) {
            if (regex.matcher(key).matches()) {
                matchingKeys.add(RespObject.bulkString(key));
            }
        }

        return RespObject.array(matchingKeys);
    }

    private RespObject handleType(Command cmd, DataStore store) {
        if (cmd.argCount() != 1) {
            return RespObject.error("ERR wrong number of arguments for 'type' command");
        }

        RedisValue val = store.get(cmd.arg(0));
        if (val == null) {
            return new RespObject.SimpleString("none");
        }

        String typeStr = switch (val.getType()) {
            case STRING -> "string";
            case LIST -> "list";
            case SET -> "set";
            case HASH -> "hash";
        };
        return new RespObject.SimpleString(typeStr);
    }

    private Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append(".");
                case '.', '(', ')', '+', '|', '^', '$', '@', '%', '\\' -> {
                    sb.append('\\').append(c);
                }
                case '[' -> {
                    int close = glob.indexOf(']', i);
                    if (close > i) {
                        sb.append(glob, i, close + 1);
                        i = close;
                    } else {
                        sb.append("\\[");
                    }
                }
                default -> sb.append(c);
            }
        }
        sb.append("$");
        return Pattern.compile(sb.toString());
    }
}
