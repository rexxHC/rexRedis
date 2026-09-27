package com.rexredis.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the RESP protocol decoder.
 */
class RespDecoderTest {

    // TODO: Test cases:
    //  - Decode Simple String "+OK\r\n"
    //  - Decode Error "-ERR unknown\r\n"
    //  - Decode Integer ":1000\r\n"
    //  - Decode Bulk String "$5\r\nhello\r\n"
    //  - Decode Null Bulk String "$-1\r\n"
    //  - Decode Array "*2\r\n$3\r\nGET\r\n$4\r\nname\r\n"
    //  - Decode Null Array "*-1\r\n"
    //  - Handle partial/incomplete data (return null)
    //  - Handle nested arrays

    @Test
    void placeholder() {
        assertThat(true).isTrue();
    }
}
