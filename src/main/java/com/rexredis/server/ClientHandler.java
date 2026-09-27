package com.rexredis.server;

/**
 * Handles I/O for a single connected client.
 * Attached to each connection's SelectionKey as an attachment.
 * Responsible for buffering partial reads, feeding complete messages
 * to the RESP decoder, and writing responses back.
 */
public class ClientHandler {

    // TODO: Implement per-connection read/write buffering
    //  - ByteBuffer for incoming data
    //  - Queue<RespObject> for outgoing responses
    //  - Track subscription state for Pub/Sub mode
}
