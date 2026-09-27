package com.rexredis;

import com.rexredis.config.ServerConfig;
import com.rexredis.server.RexRedisServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the RexRedis server.
 */
public class RexRedisApplication {

    private static final Logger logger = LoggerFactory.getLogger(RexRedisApplication.class);

    public static void main(String[] args) {
        ServerConfig config = ServerConfig.fromArgs(args);
        logger.info("Starting RexRedis on port {}", config.getPort());

        RexRedisServer server = new RexRedisServer(config);
        server.start();
    }
}
