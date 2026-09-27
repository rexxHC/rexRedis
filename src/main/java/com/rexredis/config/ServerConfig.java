package com.rexredis.config;

/**
 * Holds server configuration: port, persistence settings, etc.
 */
public class ServerConfig {

    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_RDB_FILENAME = "dump.rdb";

    private int port;
    private String rdbFilename;
    private boolean persistenceEnabled;

    public ServerConfig() {
        this.port = DEFAULT_PORT;
        this.rdbFilename = DEFAULT_RDB_FILENAME;
        this.persistenceEnabled = true;
    }

    /**
     * Parse configuration from command-line arguments.
     * Supports: --port &lt;number&gt;, --rdb &lt;filename&gt;, --no-persistence
     */
    public static ServerConfig fromArgs(String[] args) {
        ServerConfig config = new ServerConfig();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port" -> config.port = Integer.parseInt(args[++i]);
                case "--rdb" -> config.rdbFilename = args[++i];
                case "--no-persistence" -> config.persistenceEnabled = false;
            }
        }
        return config;
    }

    public int getPort() {
        return port;
    }

    public String getRdbFilename() {
        return rdbFilename;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }
}
