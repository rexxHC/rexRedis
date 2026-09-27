package com.rexredis.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Encodes RespObject instances into bytes for sending over the wire.
 */
public class RespEncoder {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    /**
     * Serializes a RespObject into a byte array.
     *
     * @param obj the RESP object to encode
     * @return the wire-format byte representation
     */
    public byte[] encode(RespObject obj) {
        // TODO: Implement RESP serialization using pattern matching
        //  switch (obj) {
        //      case RespObject.SimpleString s -> encode "+{value}\r\n"
        //      case RespObject.Error e        -> encode "-{message}\r\n"
        //      case RespObject.IntegerResp i  -> encode ":{value}\r\n"
        //      case RespObject.BulkString b   -> encode "${length}\r\n{value}\r\n" or "$-1\r\n"
        //      case RespObject.ArrayResp a    -> encode "*{count}\r\n" + each element, or "*-1\r\n"
        //  }
        return new byte[0];
    }
}
