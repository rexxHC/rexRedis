package com.rexredis.server;

import com.rexredis.command.CommandRouter;
import com.rexredis.config.ServerConfig;
import com.rexredis.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main NIO event loop server.
 * Opens a ServerSocketChannel, runs a Selector loop, and dispatches
 * I/O events to ClientHandler instances.
 */
public class RexRedisServer {

    private static final Logger logger = LoggerFactory.getLogger(RexRedisServer.class);

    private final ServerConfig config;
    private final DataStore dataStore;
    private final CommandRouter commandRouter;

    public RexRedisServer(ServerConfig config) {
        this.config = config;
        this.dataStore = new DataStore();
        this.commandRouter = new CommandRouter(dataStore);
    }

    /**
     * Starts the NIO event loop. This method blocks.
     */
    public void start() {
        // TODO: Implement NIO Selector loop
        //  1. Open ServerSocketChannel, bind to config.getPort()
        //  2. Register with Selector for OP_ACCEPT
        //  3. Loop: selector.select(), handle accept/read/write
        logger.info("RexRedisServer started on port {}", config.getPort());
    }

    public void stop() {
        // TODO: Graceful shutdown
        logger.info("RexRedisServer stopping...");
    }
}
