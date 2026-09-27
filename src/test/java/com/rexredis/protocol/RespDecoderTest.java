package com.rexredis.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RespDecoderTest {

    private RespDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new RespDecoder();
    }

    private ByteBuffer toBuffer(String s) {
        ByteBuffer buf = ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
        return buf;
    }

    @Test
    void testDecodeSimpleString() {
        ByteBuffer buf = toBuffer("+OK\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.SimpleString.class);
        assertThat(((RespObject.SimpleString) obj).value()).isEqualTo("OK");
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeError() {
        ByteBuffer buf = toBuffer("-ERR unknown command 'foo'\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.Error.class);
        assertThat(((RespObject.Error) obj).message()).isEqualTo("ERR unknown command 'foo'");
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeInteger() {
        ByteBuffer buf = toBuffer(":1000\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.IntegerResp.class);
        assertThat(((RespObject.IntegerResp) obj).value()).isEqualTo(1000L);
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeNegativeInteger() {
        ByteBuffer buf = toBuffer(":-42\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.IntegerResp.class);
        assertThat(((RespObject.IntegerResp) obj).value()).isEqualTo(-42L);
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeBulkString() {
        ByteBuffer buf = toBuffer("$5\r\nhello\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.BulkString.class);
        assertThat(((RespObject.BulkString) obj).value()).isEqualTo("hello");
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeEmptyBulkString() {
        ByteBuffer buf = toBuffer("$0\r\n\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.BulkString.class);
        assertThat(((RespObject.BulkString) obj).value()).isEqualTo("");
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeNullBulkString() {
        ByteBuffer buf = toBuffer("$-1\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.BulkString.class);
        assertThat(((RespObject.BulkString) obj).value()).isNull();
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeArray() {
        ByteBuffer buf = toBuffer("*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.ArrayResp.class);
        List<RespObject> elements = ((RespObject.ArrayResp) obj).elements();
        assertThat(elements).hasSize(2);
        assertThat(((RespObject.BulkString) elements.get(0)).value()).isEqualTo("ECHO");
        assertThat(((RespObject.BulkString) elements.get(1)).value()).isEqualTo("hello");
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeEmptyArray() {
        ByteBuffer buf = toBuffer("*0\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.ArrayResp.class);
        assertThat(((RespObject.ArrayResp) obj).elements()).isEmpty();
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodeNullArray() {
        ByteBuffer buf = toBuffer("*-1\r\n");
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isInstanceOf(RespObject.ArrayResp.class);
        assertThat(((RespObject.ArrayResp) obj).elements()).isNull();
        assertThat(buf.hasRemaining()).isFalse();
    }

    @Test
    void testDecodePartialMessageReturnsNullAndPreservesPosition() {
        // Incomplete bulk string (missing the final \r\n and byte)
        ByteBuffer buf = ByteBuffer.allocate(32);
        buf.put("$5\r\nhel".getBytes(StandardCharsets.UTF_8));
        buf.flip();

        int initialPos = buf.position();
        RespObject obj = decoder.decode(buf);

        assertThat(obj).isNull();
        assertThat(buf.position()).isEqualTo(initialPos);

        // Now append the rest
        buf.compact();
        buf.put("lo\r\n".getBytes(StandardCharsets.UTF_8));
        buf.flip();

        RespObject completeObj = decoder.decode(buf);
        assertThat(completeObj).isInstanceOf(RespObject.BulkString.class);
        assertThat(((RespObject.BulkString) completeObj).value()).isEqualTo("hello");
    }

    @Test
    void testDecodePipelinedMessages() {
        ByteBuffer buf = toBuffer("+OK\r\n:123\r\n");
        RespObject first = decoder.decode(buf);
        RespObject second = decoder.decode(buf);

        assertThat(first).isInstanceOf(RespObject.SimpleString.class);
        assertThat(((RespObject.SimpleString) first).value()).isEqualTo("OK");

        assertThat(second).isInstanceOf(RespObject.IntegerResp.class);
        assertThat(((RespObject.IntegerResp) second).value()).isEqualTo(123L);

        assertThat(buf.hasRemaining()).isFalse();
    }
}
