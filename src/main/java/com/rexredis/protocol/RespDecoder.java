package com.rexredis.protocol;

import java.nio.ByteBuffer;

/**
 * Decodes raw bytes from a ByteBuffer into RespObject instances.
 *
 * Must handle partial reads — the full RESP message may not arrive
 * in a single read() call from the socket.
 */
public class RespDecoder {

    /**
     * Attempts to decode the next complete RespObject from the buffer.
     *
     * @param buffer the buffer containing incoming bytes (in read-mode)
     * @return the decoded RespObject, or null if the buffer doesn't contain a complete message yet
     */
    public RespObject decode(ByteBuffer buffer) {
        // TODO: Implement RESP parsing
        //  1. Peek at the first byte to determine type (+, -, :, $, *)
        //  2. Parse according to type rules
        //  3. If data is incomplete, reset buffer position and return null
        return null;
    }
}
