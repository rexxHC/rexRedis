package com.rexredis.protocol;

import java.util.List;

/**
 * Represents a RESP (REdis Serialization Protocol) data type.
 *
 * RESP defines five types:
 * - Simple String: "+OK\r\n"
 * - Error:         "-ERR message\r\n"
 * - Integer:       ":1000\r\n"
 * - Bulk String:   "$5\r\nhello\r\n"  (null = "$-1\r\n")
 * - Array:         "*2\r\n..." (null = "*-1\r\n")
 */
public sealed interface RespObject {

    record SimpleString(String value) implements RespObject {}
    record Error(String message) implements RespObject {}
    record IntegerResp(long value) implements RespObject {}
    record BulkString(String value) implements RespObject {}  // value == null for null bulk string
    record ArrayResp(List<RespObject> elements) implements RespObject {} // elements == null for null array

    // ---- Factory methods for convenience ----

    static RespObject ok() {
        return new SimpleString("OK");
    }

    static RespObject error(String message) {
        return new Error(message);
    }

    static RespObject integer(long value) {
        return new IntegerResp(value);
    }

    static RespObject bulkString(String value) {
        return new BulkString(value);
    }

    static RespObject nullBulkString() {
        return new BulkString(null);
    }

    static RespObject array(List<RespObject> elements) {
        return new ArrayResp(elements);
    }

    static RespObject nullArray() {
        return new ArrayResp(null);
    }
}
