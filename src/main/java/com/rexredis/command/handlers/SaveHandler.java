package com.rexredis.command.handlers;

import com.rexredis.command.Command;
import com.rexredis.config.ServerConfig;
import com.rexredis.persistence.RdbSaver;
import com.rexredis.protocol.RespObject;
import com.rexredis.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles SAVE and BGSAVE commands.
 */
public class SaveHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(SaveHandler.class);
    private final ServerConfig config;
    private final RdbSaver saver;

    public SaveHandler() {
        this(new ServerConfig());
    }

    public SaveHandler(ServerConfig config) {
        this.config = config;
        this.saver = new RdbSaver();
    }

    @Override
    public RespObject handle(Command cmd, DataStore store) {
        String name = cmd.name().toUpperCase();
        String filename = config.getRdbFilename();

        if ("BGSAVE".equals(name)) {
            Thread.ofVirtual().start(() -> {
                try {
                    saver.save(store, filename);
                    logger.info("Background RDB snapshot saved to {}", filename);
                } catch (IOException e) {
                    logger.error("Background RDB save failed", e);
                }
            });
            return new RespObject.SimpleString("Background saving started");
        }

        // Synchronous SAVE
        try {
            saver.save(store, filename);
            logger.info("RDB snapshot saved to {}", filename);
            return RespObject.ok();
        } catch (IOException e) {
            logger.error("RDB save failed", e);
            return RespObject.error("ERR failed to save RDB snapshot: " + e.getMessage());
        }
    }
}
