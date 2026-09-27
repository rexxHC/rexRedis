package com.rexredis.protocol;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes raw bytes from a ByteBuffer into RespObject instances.
 *
 * <p>Handles partial reads gracefully — if the buffer doesn't contain a complete
 * RESP message, the buffer position is reset and {@code null} is returned.
 * The caller should accumulate more data and retry.
 *
 * <p>RESP type prefixes:
 * <ul>
 *   <li>{@code +} — Simple String</li>
 *   <li>{@code -} — Error</li>
 *   <li>{@code :} — Integer</li>
 *   <li>{@code $} — Bulk String</li>
 *   <li>{@code *} — Array</li>
 * </ul>
 */
public class RespDecoder {

    /**
     * Attempts to decode the next complete RespObject from the buffer.
     * The buffer must be in read mode (after {@code flip()}).
     *
     * @param buffer the buffer containing incoming bytes
     * @return the decoded RespObject, or {@code null} if data is incomplete
     * @throws IllegalArgumentException if the data contains an unknown RESP type prefix
     */
    public RespObject decode(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return null;
        }

        int savedPosition = buffer.position();
        try {
            return doDecode(buffer);
        } catch (BufferUnderflowException e) {
            // Incomplete message — rewind so the caller can accumulate more data
            buffer.position(savedPosition);
            return null;
        }
    }

    private RespObject doDecode(ByteBuffer buffer) {
        byte prefix = readByte(buffer);
        return switch (prefix) {
            case '+' -> decodeSimpleString(buffer);
            case '-' -> decodeError(buffer);
            case ':' -> decodeInteger(buffer);
            case '$' -> decodeBulkString(buffer);
            case '*' -> decodeArray(buffer);
            default -> throw new IllegalArgumentException(
                    "Unknown RESP type prefix: '" + (char) prefix + "' (0x" + Integer.toHexString(prefix) + ")");
        };
    }

    // ── Simple String: +OK\r\n ──────────────────────────────────────────

    private RespObject decodeSimpleString(ByteBuffer buffer) {
        return new RespObject.SimpleString(readLine(buffer));
    }

    // ── Error: -ERR message\r\n ─────────────────────────────────────────

    private RespObject decodeError(ByteBuffer buffer) {
        return new RespObject.Error(readLine(buffer));
    }

    // ── Integer: :1000\r\n ──────────────────────────────────────────────

    private RespObject decodeInteger(ByteBuffer buffer) {
        String line = readLine(buffer);
        return new RespObject.IntegerResp(Long.parseLong(line));
    }

    // ── Bulk String: $5\r\nhello\r\n  or  $-1\r\n (null) ───────────────

    private RespObject decodeBulkString(ByteBuffer buffer) {
        String lengthLine = readLine(buffer);
        int length = Integer.parseInt(lengthLine);

        if (length == -1) {
            return RespObject.nullBulkString();
        }

        if (length < 0) {
            throw new IllegalArgumentException("Invalid bulk string length: " + length);
        }

        // Read exactly 'length' bytes
        if (buffer.remaining() < length + 2) {  // +2 for trailing \r\n
            throw new BufferUnderflowException();
        }

        byte[] data = new byte[length];
        buffer.get(data);

        // Consume the trailing \r\n
        byte cr = readByte(buffer);
        byte lf = readByte(buffer);
        if (cr != '\r' || lf != '\n') {
            throw new IllegalArgumentException("Bulk string not terminated with \\r\\n");
        }

        return new RespObject.BulkString(new String(data, StandardCharsets.UTF_8));
    }

    // ── Array: *2\r\n$3\r\nGET\r\n$4\r\nname\r\n  or  *-1\r\n (null) ──

    private RespObject decodeArray(ByteBuffer buffer) {
        String countLine = readLine(buffer);
        int count = Integer.parseInt(countLine);

        if (count == -1) {
            return RespObject.nullArray();
        }

        if (count < 0) {
            throw new IllegalArgumentException("Invalid array count: " + count);
        }

        List<RespObject> elements = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            elements.add(doDecode(buffer));  // recursive — arrays can be nested
        }

        return new RespObject.ArrayResp(elements);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Reads a single byte, throwing {@link BufferUnderflowException} if empty.
     */
    private byte readByte(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new BufferUnderflowException();
        }
        return buffer.get();
    }

    /**
     * Reads bytes until {@code \r\n} is found and returns them as a String.
     * The {@code \r\n} is consumed but not included in the result.
     *
     * @throws BufferUnderflowException if the buffer runs out before finding {@code \r\n}
     */
    private String readLine(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            byte b = readByte(buffer);
            if (b == '\r') {
                byte next = readByte(buffer);
                if (next == '\n') {
                    return sb.toString();
                }
                // Not \r\n — treat as regular data (shouldn't happen in valid RESP)
                sb.append((char) b);
                sb.append((char) next);
            } else {
                sb.append((char) b);
            }
        }
    }
}
