package com.rexredis.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RespEncoderTest {

    private RespEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new RespEncoder();
    }

    private String asString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test
    void testEncodeSimpleString() {
        byte[] encoded = encoder.encode(new RespObject.SimpleString("OK"));
        assertThat(asString(encoded)).isEqualTo("+OK\r\n");
    }

    @Test
    void testEncodeError() {
        byte[] encoded = encoder.encode(new RespObject.Error("ERR unknown command"));
        assertThat(asString(encoded)).isEqualTo("-ERR unknown command\r\n");
    }

    @Test
    void testEncodeInteger() {
        byte[] encoded = encoder.encode(new RespObject.IntegerResp(42));
        assertThat(asString(encoded)).isEqualTo(":42\r\n");
    }

    @Test
    void testEncodeNegativeInteger() {
        byte[] encoded = encoder.encode(new RespObject.IntegerResp(-100));
        assertThat(asString(encoded)).isEqualTo(":-100\r\n");
    }

    @Test
    void testEncodeBulkString() {
        byte[] encoded = encoder.encode(new RespObject.BulkString("hello"));
        assertThat(asString(encoded)).isEqualTo("$5\r\nhello\r\n");
    }

    @Test
    void testEncodeEmptyBulkString() {
        byte[] encoded = encoder.encode(new RespObject.BulkString(""));
        assertThat(asString(encoded)).isEqualTo("$0\r\n\r\n");
    }

    @Test
    void testEncodeNullBulkString() {
        byte[] encoded = encoder.encode(RespObject.nullBulkString());
        assertThat(asString(encoded)).isEqualTo("$-1\r\n");
    }

    @Test
    void testEncodeArray() {
        RespObject array = RespObject.array(List.of(
                RespObject.bulkString("SET"),
                RespObject.bulkString("foo"),
                RespObject.bulkString("bar")
        ));
        byte[] encoded = encoder.encode(array);
        assertThat(asString(encoded)).isEqualTo("*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n");
    }

    @Test
    void testEncodeNullArray() {
        byte[] encoded = encoder.encode(RespObject.nullArray());
        assertThat(asString(encoded)).isEqualTo("*-1\r\n");
    }

    @Test
    void testRoundTrip() {
        RespDecoder decoder = new RespDecoder();
        RespObject original = RespObject.array(List.of(
                new RespObject.SimpleString("PONG"),
                new RespObject.IntegerResp(999),
                RespObject.bulkString("world")
        ));

        byte[] encoded = encoder.encode(original);
        RespObject decoded = decoder.decode(java.nio.ByteBuffer.wrap(encoded));

        assertThat(decoded).isEqualTo(original);
    }
}
