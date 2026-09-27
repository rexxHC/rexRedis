package com.rexredis.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Encodes RespObject instances into their RESP wire-format byte representation.
 *
 * <p>Each RESP type has a specific encoding:
 * <ul>
 *   <li>Simple String: {@code +value\r\n}</li>
 *   <li>Error: {@code -message\r\n}</li>
 *   <li>Integer: {@code :value\r\n}</li>
 *   <li>Bulk String: {@code $length\r\nvalue\r\n} or {@code $-1\r\n} for null</li>
 *   <li>Array: {@code *count\r\n...elements...} or {@code *-1\r\n} for null</li>
 * </ul>
 */
public class RespEncoder {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    /**
     * Serializes a RespObject into its RESP wire-format bytes.
     *
     * @param obj the RESP object to encode
     * @return the byte representation ready to send over the wire
     */
    public byte[] encode(RespObject obj) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeObject(out, obj);
        return out.toByteArray();
    }

    private void writeObject(ByteArrayOutputStream out, RespObject obj) {
        switch (obj) {
            case RespObject.SimpleString s -> encodeSimpleString(out, s);
            case RespObject.Error e        -> encodeError(out, e);
            case RespObject.IntegerResp i  -> encodeInteger(out, i);
            case RespObject.BulkString b   -> encodeBulkString(out, b);
            case RespObject.ArrayResp a    -> encodeArray(out, a);
        }
    }

    // ── +OK\r\n ─────────────────────────────────────────────────────────

    private void encodeSimpleString(ByteArrayOutputStream out, RespObject.SimpleString s) {
        out.write('+');
        writeBytes(out, s.value());
        writeCrlf(out);
    }

    // ── -ERR message\r\n ────────────────────────────────────────────────

    private void encodeError(ByteArrayOutputStream out, RespObject.Error e) {
        out.write('-');
        writeBytes(out, e.message());
        writeCrlf(out);
    }

    // ── :1000\r\n ───────────────────────────────────────────────────────

    private void encodeInteger(ByteArrayOutputStream out, RespObject.IntegerResp i) {
        out.write(':');
        writeBytes(out, Long.toString(i.value()));
        writeCrlf(out);
    }

    // ── $5\r\nhello\r\n  or  $-1\r\n ────────────────────────────────────

    private void encodeBulkString(ByteArrayOutputStream out, RespObject.BulkString b) {
        out.write('$');
        if (b.value() == null) {
            writeBytes(out, "-1");
            writeCrlf(out);
        } else {
            byte[] data = b.value().getBytes(StandardCharsets.UTF_8);
            writeBytes(out, Integer.toString(data.length));
            writeCrlf(out);
            out.writeBytes(data);
            writeCrlf(out);
        }
    }

    // ── *2\r\n...  or  *-1\r\n ──────────────────────────────────────────

    private void encodeArray(ByteArrayOutputStream out, RespObject.ArrayResp a) {
        out.write('*');
        List<RespObject> elements = a.elements();
        if (elements == null) {
            writeBytes(out, "-1");
            writeCrlf(out);
        } else {
            writeBytes(out, Integer.toString(elements.size()));
            writeCrlf(out);
            for (RespObject element : elements) {
                writeObject(out, element);
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void writeBytes(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    private void writeCrlf(ByteArrayOutputStream out) {
        out.writeBytes(CRLF);
    }
}
