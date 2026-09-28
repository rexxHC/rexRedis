package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

import java.util.ArrayList;
import java.util.List;


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

        String pattern = cmd.arg(0);

        List<RespObject> matchingKeys = new ArrayList<>();
        for (String key : store.keys()) {
            if (globMatch(pattern, key)) {
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

    /**
     * Two-pointer glob matcher supporting *, ?, [...], and \\ escaping.
     * No regex, so no backtracking bombs or unescaped-character crashes.
     */
    private boolean globMatch(String pattern, String text) {
        int pi = 0, ti = 0;
        int starPi = -1, starTi = -1;

        while (ti < text.length()) {
            if (pi < pattern.length() && pattern.charAt(pi) == '\\' && pi + 1 < pattern.length()) {
                if (text.charAt(ti) == pattern.charAt(pi + 1)) {
                    pi += 2;
                    ti++;
                } else if (starPi >= 0) {
                    pi = starPi;
                    ti = ++starTi;
                } else {
                    return false;
                }
            } else if (pi < pattern.length() && pattern.charAt(pi) == '?') {
                pi++;
                ti++;
            } else if (pi < pattern.length() && pattern.charAt(pi) == '*') {
                starPi = pi;
                starTi = ti;
                pi++;
            } else if (pi < pattern.length() && pattern.charAt(pi) == '[') {
                int close = pattern.indexOf(']', pi + 1);
                if (close < 0) {
                    // No closing bracket — treat '[' as literal
                    if (text.charAt(ti) == '[') {
                        pi++;
                        ti++;
                    } else if (starPi >= 0) {
                        pi = starPi;
                        ti = ++starTi;
                    } else {
                        return false;
                    }
                } else {
                    boolean negate = (pi + 1 < close && pattern.charAt(pi + 1) == '^');
                    int rangeStart = negate ? pi + 2 : pi + 1;
                    boolean matched = false;
                    for (int ri = rangeStart; ri < close; ri++) {
                        if (ri + 2 < close && pattern.charAt(ri + 1) == '-') {
                            if (text.charAt(ti) >= pattern.charAt(ri) && text.charAt(ti) <= pattern.charAt(ri + 2)) {
                                matched = true;
                            }
                            ri += 2;
                        } else {
                            if (text.charAt(ti) == pattern.charAt(ri)) {
                                matched = true;
                            }
                        }
                    }
                    if (negate) matched = !matched;
                    if (!matched) {
                        if (starPi >= 0) {
                            pi = starPi;
                            ti = ++starTi;
                            continue;
                        }
                        return false;
                    }
                    pi = close + 1;
                    ti++;
                }
            } else if (pi < pattern.length() && pattern.charAt(pi) == text.charAt(ti)) {
                pi++;
                ti++;
            } else if (starPi >= 0) {
                pi = starPi;
                ti = ++starTi;
            } else {
                return false;
            }
        }

        while (pi < pattern.length() && pattern.charAt(pi) == '*') pi++;
        return pi == pattern.length();
    }
}
