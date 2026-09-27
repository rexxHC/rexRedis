package com.rexredis;

import com.rexredis.config.ServerConfig;
import com.rexredis.server.RexRedisServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the RexRedis server.
 *
 * <p>Usage:
 * <pre>
 *   java -jar rexredis.jar                          # default port 6379
 *   java -jar rexredis.jar --port 6380              # custom port
 *   java -jar rexredis.jar --no-persistence          # disable RDB
 * </pre>
 */
public class RexRedisApplication {

    private static final Logger logger = LoggerFactory.getLogger(RexRedisApplication.class);

    private static final String BANNER = """
            
             ██████╗ ███████╗██╗  ██╗██████╗ ███████╗██████╗ ██╗███████╗
             ██╔══██╗██╔════╝╚██╗██╔╝██╔══██╗██╔════╝██╔══██╗██║██╔════╝
             ██████╔╝█████╗   ╚███╔╝ ██████╔╝█████╗  ██║  ██║██║███████╗
             ██╔══██╗██╔══╝   ██╔██╗ ██╔══██╗██╔══╝  ██║  ██║██║╚════██║
             ██║  ██║███████╗██╔╝ ██╗██║  ██║███████╗██████╔╝██║███████║
             ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═════╝ ╚═╝╚══════╝
                                                           v1.0.0
            """;

    public static void main(String[] args) {
        System.out.println(BANNER);

        ServerConfig config = ServerConfig.fromArgs(args);
        logger.info("Configuration: port={}, persistence={}, rdb={}",
                config.getPort(), config.isPersistenceEnabled(), config.getRdbFilename());

        RexRedisServer server = new RexRedisServer(config);
        server.start();
    }
}
