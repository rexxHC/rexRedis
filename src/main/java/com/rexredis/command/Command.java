package com.rexredis.command;

import com.rexredis.protocol.RespObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed Redis command with its name and arguments.
 *
 * <p>Redis commands arrive as RESP Arrays of Bulk Strings. For example,
 * {@code SET name rex} is sent as:
 * <pre>
 * *3\r\n$3\r\nSET\r\n$4\r\nname\r\n$3\r\nrex\r\n
 * </pre>
 *
 * @param name the command name in UPPERCASE (e.g., "SET", "GET")
 * @param args the command arguments (e.g., ["name", "rex"])
 */
public record Command(String name, List<String> args) {

    /**
     * Creates a Command from a decoded RESP object (expected to be an Array of Bulk Strings).
     *
     * @param obj the decoded RESP object from the client
     * @return the parsed Command
     * @throws IllegalArgumentException if the object is not a valid command format
     */
    public static Command fromRespObject(RespObject obj) {
        if (!(obj instanceof RespObject.ArrayResp array)) {
            throw new IllegalArgumentException("Expected RESP Array, got: " + obj.getClass().getSimpleName());
        }

        List<RespObject> elements = array.elements();
        if (elements == null || elements.isEmpty()) {
            throw new IllegalArgumentException("Empty command array");
        }

        // First element is the command name
        String name = extractString(elements.get(0)).toUpperCase();

        // Remaining elements are the arguments
        List<String> args = new ArrayList<>(elements.size() - 1);
        for (int i = 1; i < elements.size(); i++) {
            args.add(extractString(elements.get(i)));
        }

        return new Command(name, args);
    }

    private static String extractString(RespObject obj) {
        return switch (obj) {
            case RespObject.BulkString b -> {
                if (b.value() == null) {
                    throw new IllegalArgumentException("Null bulk string in command");
                }
                yield b.value();
            }
            case RespObject.SimpleString s -> s.value();
            default -> throw new IllegalArgumentException(
                    "Expected string in command, got: " + obj.getClass().getSimpleName());
        };
    }

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
