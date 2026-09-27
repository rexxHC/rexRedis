package com.rexredis.server;

import com.rexredis.command.CommandRouter;
import com.rexredis.config.ServerConfig;
import com.rexredis.persistence.RdbLoader;
import com.rexredis.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * The main NIO event loop server — the heart of RexRedis.
 *
 * <p>Uses a single-threaded {@link Selector} loop (like real Redis) to multiplex
 * thousands of client connections without any locks or context switching.
 *
 * <p>Event loop flow:
 * <ol>
 *   <li>{@code selector.select()} — block until I/O is ready on any channel</li>
 *   <li>For each ready key:
 *     <ul>
 *       <li>{@code OP_ACCEPT} — accept new client, register for {@code OP_READ}</li>
 *       <li>{@code OP_READ} — delegate to {@link ClientHandler#handleRead()}</li>
 *       <li>{@code OP_WRITE} — delegate to {@link ClientHandler#handleWrite()}</li>
 *     </ul>
 *   </li>
 *   <li>Run active expiry cycle (Phase 3)</li>
 *   <li>Repeat</li>
 * </ol>
 */
public class RexRedisServer {

    private static final Logger logger = LoggerFactory.getLogger(RexRedisServer.class);

    private final ServerConfig config;
    private final DataStore dataStore;
    private final CommandRouter commandRouter;

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = false;
    private int boundPort;
    private final java.util.concurrent.CountDownLatch startupLatch = new java.util.concurrent.CountDownLatch(1);

    public RexRedisServer(ServerConfig config) {
        this.config = config;
        this.dataStore = new DataStore();
        this.commandRouter = new CommandRouter(dataStore, config);
    }

    /**
     * Starts the NIO event loop. This method blocks until {@link #stop()} is called.
     */
    public void start() {
        try {
            loadPersistedData();
            initServer();
            registerShutdownHook();

            running = true;
            startupLatch.countDown();
            logger.info("RexRedis is ready to accept connections on port {}", boundPort);

            eventLoop();
        } catch (IOException e) {
            startupLatch.countDown();
            logger.error("Fatal server error", e);
            throw new RuntimeException("Server failed to start", e);
        }
    }

    /**
     * Signals the event loop to stop.
     */
    public void stop() {
        running = false;
        if (selector != null) {
            selector.wakeup();  // Break out of select() if it's blocking
        }
    }

    // ── Initialization ──────────────────────────────────────────────────

    private void initServer() throws IOException {
        selector = Selector.open();

        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(config.getPort()));
        this.boundPort = ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        logger.info("Server socket bound to port {}", this.boundPort);
    }

    private void loadPersistedData() {
        if (!config.isPersistenceEnabled()) {
            return;
        }

        java.io.File rdbFile = new java.io.File(config.getRdbFilename());
        if (rdbFile.exists()) {
            try {
                new RdbLoader().load(config.getRdbFilename(), dataStore);
                logger.info("Loaded RDB snapshot: {} keys", dataStore.size());
            } catch (IOException e) {
                logger.warn("Failed to load RDB file '{}': {}", config.getRdbFilename(), e.getMessage());
            }
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered — stopping RexRedis");
            if (config.isPersistenceEnabled()) {
                try {
                    new com.rexredis.persistence.RdbSaver().save(dataStore, config.getRdbFilename());
                    logger.info("Saved RDB snapshot on shutdown to {}", config.getRdbFilename());
                } catch (IOException e) {
                    logger.error("Failed to save RDB snapshot on shutdown", e);
                }
            }
            stop();
        }, "rexredis-shutdown"));
    }

    // ── Event Loop ──────────────────────────────────────────────────────

    private void eventLoop() throws IOException {
        while (running) {
            // Block until at least one channel is ready, or wakeup() is called
            // Timeout of 100ms allows periodic tasks (like active expiry) to run
            int readyCount = selector.select(100);

            if (!running) {
                break;
            }

            if (readyCount > 0) {
                processSelectedKeys();
            }

            // Periodic tasks (Phase 3: active expiry cycle)
            dataStore.getExpiryManager().activeExpiryCycle(dataStore);
        }

        cleanup();
    }

    private void processSelectedKeys() {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iter = selectedKeys.iterator();

        while (iter.hasNext()) {
            SelectionKey key = iter.next();
            iter.remove();  // Must remove manually — Selector doesn't auto-clear

            if (!key.isValid()) {
                continue;
            }

            try {
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                if (key.isValid() && key.isWritable()) {
                    handleWrite(key);
                }
            } catch (IOException e) {
                logger.debug("Client disconnected: {}", e.getMessage());
                closeClient(key);
            }
        }
    }

    // ── Accept / Read / Write ───────────────────────────────────────────

    private void handleAccept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) {
            return;  // Spurious wakeup
        }

        clientChannel.configureBlocking(false);

        // Register for OP_READ and attach a ClientHandler
        SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
        ClientHandler handler = new ClientHandler(clientChannel, clientKey, commandRouter);
        clientKey.attach(handler);

        logger.info("Client connected: {}", clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        handler.handleRead();
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        handler.handleWrite();
    }

    private void closeClient(SelectionKey key) {
        ClientHandler handler = (ClientHandler) key.attachment();
        if (handler != null) {
            try {
                logger.info("Closing client: {}", handler.getChannel().getRemoteAddress());
            } catch (IOException ignored) {
            }
            handler.close();
        }
        key.cancel();
    }

    // ── Cleanup ─────────────────────────────────────────────────────────

    private void cleanup() {
        logger.info("Cleaning up server resources...");
        try {
            // Close all client connections
            for (SelectionKey key : selector.keys()) {
                closeClient(key);
            }
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (IOException e) {
            logger.error("Error during cleanup", e);
        }
        logger.info("RexRedis shut down.");
    }

    // ── Accessors (for testing) ─────────────────────────────────────────

    public DataStore getDataStore() {
        return dataStore;
    }

    public boolean isRunning() {
        return running;
    }

    public int getBoundPort() {
        return boundPort;
    }

    public void awaitRunning() throws InterruptedException {
        startupLatch.await();
    }
}
