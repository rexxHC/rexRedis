package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.config.ServerConfig;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SaveHandlerTest {

    private DataStore store;
    private ServerConfig config;
    private SaveHandler handler;
    private Path rdbFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        store = new DataStore();
        rdbFile = tempDir.resolve("test_dump.rdb");

        config = new ServerConfig();
        config.setRdbFilename(rdbFile.toString());
        handler = new SaveHandler(config);
    }

    @Test
    void testSaveCommand() {
        store.set("app", RedisValue.string("rexredis"));
        RespObject res = handler.handle(new Command("SAVE", List.of()), store);

        assertThat(res).isEqualTo(RespObject.ok());
        assertThat(Files.exists(rdbFile)).isTrue();
    }

    @Test
    void testBgsaveCommand() throws InterruptedException {
        store.set("bgKey", RedisValue.string("bgVal"));
        RespObject res = handler.handle(new Command("BGSAVE", List.of()), store);

        assertThat(res).isInstanceOf(RespObject.SimpleString.class);
        assertThat(((RespObject.SimpleString) res).value()).isEqualTo("Background saving started");

        // Wait a moment for virtual thread to finish
        Thread.sleep(100);
        assertThat(Files.exists(rdbFile)).isTrue();
    }
}
