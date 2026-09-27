package com.rexredis.server;

import com.rexredis.command.Command;
import com.rexredis.command.CommandRouter;
import com.rexredis.protocol.RespDecoder;
import com.rexredis.protocol.RespEncoder;
import com.rexredis.protocol.RespObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Handles I/O for a single connected client.
 *
 * <p>Each ClientHandler is attached to a {@link SelectionKey} and manages:
 * <ul>
 *   <li>A read buffer that accumulates incoming bytes from the socket</li>
 *   <li>A RESP decoder that parses complete messages from the read buffer</li>
 *   <li>A queue of outgoing RESP responses waiting to be written</li>
 * </ul>
 *
 * <p>The lifecycle for each message:
 * <ol>
 *   <li>Bytes arrive → appended to {@code readBuffer}</li>
 *   <li>Decoder tries to parse a complete {@link RespObject}</li>
 *   <li>If complete, it's converted to a {@link Command} and dispatched via the router</li>
 *   <li>The response is queued and the key is flagged for {@code OP_WRITE}</li>
 *   <li>On the next write-ready cycle, queued responses are flushed to the socket</li>
 * </ol>
 */
public class ClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final int READ_BUFFER_SIZE = 4096;

    private final SocketChannel channel;
    private final SelectionKey selectionKey;
    private final CommandRouter commandRouter;
    private final RespDecoder decoder;
    private final RespEncoder encoder;

    /** Buffer for accumulating incoming bytes. Always in WRITE mode between reads. */
    private ByteBuffer readBuffer;

    /** Outgoing responses waiting to be written to the socket. */
    private final Queue<byte[]> writeQueue;

    /** Partially-written response (when a single write didn't flush everything). */
    private ByteBuffer currentWriteBuffer;

    public ClientHandler(SocketChannel channel, SelectionKey selectionKey, CommandRouter commandRouter) {
        this.channel = channel;
        this.selectionKey = selectionKey;
        this.commandRouter = commandRouter;
        this.decoder = new RespDecoder();
        this.encoder = new RespEncoder();
        this.readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
        this.writeQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }

    /**
     * Called when the selector indicates this channel is readable.
     * Reads bytes from the socket, attempts to decode commands, and queues responses.
     *
     * @throws IOException if the read fails or the client disconnects
     */
    public void handleRead() throws IOException {
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            throw new IOException("Client disconnected");
        }
        if (bytesRead == 0) {
            return;
        }

        // Switch to read mode to feed the decoder
        readBuffer.flip();

        // Decode all complete messages in the buffer
        RespObject obj;
        while ((obj = decoder.decode(readBuffer)) != null) {
            processMessage(obj);
        }

        // Compact: move unread bytes to the start, switch back to write mode
        readBuffer.compact();

        // If the buffer is full (a very large command), grow it
        if (!readBuffer.hasRemaining()) {
            growReadBuffer();
        }
    }

    /**
     * Called when the selector indicates this channel is writable.
     * Flushes queued responses to the socket.
     *
     * @throws IOException if the write fails
     */
    public void handleWrite() throws IOException {
        while (true) {
            // If we have a partially-written buffer, finish it first
            if (currentWriteBuffer != null) {
                channel.write(currentWriteBuffer);
                if (currentWriteBuffer.hasRemaining()) {
                    // Socket buffer is full — we'll continue on the next write-ready cycle
                    return;
                }
                currentWriteBuffer = null;
            }

            // Pull the next response from the queue
            byte[] data = writeQueue.poll();
            if (data == null) {
                // Nothing left to write — stop listening for OP_WRITE
                selectionKey.interestOps(SelectionKey.OP_READ);
                return;
            }

            currentWriteBuffer = ByteBuffer.wrap(data);
            channel.write(currentWriteBuffer);
            if (currentWriteBuffer.hasRemaining()) {
                return;  // partial write — continue next cycle
            }
            currentWriteBuffer = null;
        }
    }

    /**
     * Processes a decoded RESP message: parses it into a Command, dispatches it,
     * and queues the response.
     */
    private void processMessage(RespObject obj) {
        try {
            Command cmd = Command.fromRespObject(obj);
            logger.debug("Received command: {} {}", cmd.name(), cmd.args());

            RespObject response = commandRouter.dispatch(cmd, this);
            if (response != null) {
                sendResponse(response);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid command format: {}", e.getMessage());
            sendResponse(RespObject.error("ERR " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing command", e);
            sendResponse(RespObject.error("ERR internal error"));
        }
    }

    /**
     * Encodes a RESP response and adds it to the thread-safe write queue.
     * Registers interest in OP_WRITE and wakes the selector if necessary.
     */
    public synchronized void sendResponse(RespObject response) {
        byte[] encoded = encoder.encode(response);
        writeQueue.add(encoded);
        if (selectionKey.isValid()) {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
            if (selectionKey.selector() != null) {
                selectionKey.selector().wakeup();
            }
        }
    }

    /**
     * Doubles the read buffer capacity, preserving any unread data.
     */
    private void growReadBuffer() {
        ByteBuffer newBuffer = ByteBuffer.allocate(readBuffer.capacity() * 2);
        readBuffer.flip();
        newBuffer.put(readBuffer);
        readBuffer = newBuffer;
        logger.debug("Grew read buffer to {} bytes", newBuffer.capacity());
    }

    /**
     * Closes this client's channel and removes all channel subscriptions.
     */
    public void close() {
        if (commandRouter.getPubSubManager() != null) {
            commandRouter.getPubSubManager().unsubscribeAll(this);
        }
        try {
            channel.close();
        } catch (IOException e) {
            logger.debug("Error closing client channel", e);
        }
    }

    public SocketChannel getChannel() {
        return channel;
    }
}
