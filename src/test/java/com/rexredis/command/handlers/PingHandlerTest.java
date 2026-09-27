package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PingHandlerTest {

    private PingHandler pingHandler;
    private EchoHandler echoHandler;
    private DataStore dataStore;

    @BeforeEach
    void setUp() {
        pingHandler = new PingHandler();
        echoHandler = new EchoHandler();
        dataStore = new DataStore();
    }

    @Test
    void testPingNoArgsReturnsPong() {
        Command cmd = new Command("PING", List.of());
        RespObject response = pingHandler.handle(cmd, dataStore);

        assertThat(response).isInstanceOf(RespObject.SimpleString.class);
        assertThat(((RespObject.SimpleString) response).value()).isEqualTo("PONG");
    }

    @Test
    void testPingWithMessageReturnsBulkString() {
        Command cmd = new Command("PING", List.of("hello"));
        RespObject response = pingHandler.handle(cmd, dataStore);

        assertThat(response).isInstanceOf(RespObject.BulkString.class);
        assertThat(((RespObject.BulkString) response).value()).isEqualTo("hello");
    }

    @Test
    void testEchoReturnsMessage() {
        Command cmd = new Command("ECHO", List.of("hey redis"));
        RespObject response = echoHandler.handle(cmd, dataStore);

        assertThat(response).isInstanceOf(RespObject.BulkString.class);
        assertThat(((RespObject.BulkString) response).value()).isEqualTo("hey redis");
    }

    @Test
    void testEchoWithoutArgsReturnsError() {
        Command cmd = new Command("ECHO", List.of());
        RespObject response = echoHandler.handle(cmd, dataStore);

        assertThat(response).isInstanceOf(RespObject.Error.class);
        assertThat(((RespObject.Error) response).message()).contains("wrong number of arguments");
    }
}
