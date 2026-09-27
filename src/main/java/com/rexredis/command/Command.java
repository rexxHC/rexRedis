package com.rexredis.command;

import java.util.List;

/**
 * Represents a parsed Redis command with its name and arguments.
 *
 * @param name the command name (e.g., "SET", "GET")
 * @param args the command arguments (e.g., ["key", "value"])
 */
public record Command(String name, List<String> args) {

    /**
     * Returns the argument at the given index.
     *
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public String arg(int index) {
        return args.get(index);
    }

    /**
     * Returns the number of arguments (excluding the command name).
     */
    public int argCount() {
        return args.size();
    }
}
