# 🦖 RexRedis — Build Your Own Redis in Java

A from-scratch implementation of a Redis-compatible in-memory data store, built in Java. This project covers the RESP protocol, an event-driven server, core data structures, persistence, and more.

---

## Table of Contents

- [Why Build Your Own Redis?](#why-build-your-own-redis)
- [High-Level Architecture](#high-level-architecture)
- [Project Structure](#project-structure)
- [Implementation Roadmap](#implementation-roadmap)
  - [Phase 1 — TCP Server & RESP Protocol](#phase-1--tcp-server--resp-protocol)
  - [Phase 2 — Core Commands & Data Structures](#phase-2--core-commands--data-structures)
  - [Phase 3 — TTL & Expiry](#phase-3--ttl--expiry)
  - [Phase 4 — Persistence (RDB Snapshots)](#phase-4--persistence-rdb-snapshots)
  - [Phase 5 — Pub/Sub](#phase-5--pubsub)
  - [Phase 6 — Advanced Features](#phase-6--advanced-features)
- [Key Concepts Explained](#key-concepts-explained)
- [How to Build & Run](#how-to-build--run)
- [Testing](#testing)

---

## Why Build Your Own Redis?

Building your own Redis teaches you:

| Concept | What You'll Learn |
|---|---|
| **Networking** | Non-blocking I/O, TCP servers, Java NIO |
| **Protocol Design** | Parsing and serializing the RESP wire protocol |
| **Data Structures** | Hash tables, skip lists, linked lists at the systems level |
| **Persistence** | Snapshotting in-memory state to disk (RDB) |
| **Concurrency** | Single-threaded event loops vs. multi-threaded approaches |
| **Systems Programming** | Memory management, expiry mechanisms, timers |

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                   RexRedis Server                   │
│                                                     │
│  ┌───────────┐    ┌────────────┐    ┌────────────┐  │
│  │  NIO      │───▶│   RESP     │───▶│  Command   │  │
│  │  Event    │    │  Protocol  │    │  Router    │  │
│  │  Loop     │    │  Codec     │    │            │  │
│  └───────────┘    └────────────┘    └─────┬──────┘  │
│                                           │         │
│                                    ┌──────▼──────┐  │
│                                    │   Command   │  │
│                                    │  Handlers   │  │
│                                    │  (GET, SET, │  │
│                                    │   DEL, ...) │  │
│                                    └──────┬──────┘  │
│                                           │         │
│  ┌───────────┐    ┌────────────┐   ┌──────▼──────┐  │
│  │ Expiry    │───▶│ Persistence│   │   Data      │  │
│  │ Manager   │    │ (RDB)      │◀──│   Store     │  │
│  └───────────┘    └────────────┘   └─────────────┘  │
└─────────────────────────────────────────────────────┘
```

**Data flow:**

1. **Client** connects via TCP to the NIO event loop.
2. Raw bytes are decoded by the **RESP Codec** into structured commands.
3. The **Command Router** dispatches to the correct **Command Handler**.
4. The handler reads/writes the **Data Store** (in-memory maps & structures).
5. The **Expiry Manager** lazily and actively evicts expired keys.
6. The **Persistence** layer periodically snapshots state to disk.

---

## Project Structure

```
rexRedis
|   pom.xml
|   README.md
|
\---src
    +---main
    |   \---java
    |       \---com
    |           \---rexredis
    |               |   RexRedisApplication.java          # Entry point — boots the server
    |               |
    |               +---command
    |               |   |   Command.java                  # Parsed command (name + args)
    |               |   |   CommandRouter.java             # Maps command name → handler
    |               |   |
    |               |   \---handlers
    |               |           CommandHandler.java        # Interface all handlers implement
    |               |           DelHandler.java            # DEL key [key ...]
    |               |           EchoHandler.java           # ECHO <msg>
    |               |           ExistsHandler.java         # EXISTS key [key ...]
    |               |           ExpireHandler.java         # EXPIRE / TTL
    |               |           GetHandler.java            # GET key
    |               |           HsetHandler.java           # HSET / HGET / HGETALL
    |               |           IncrHandler.java           # INCR / DECR
    |               |           InfoHandler.java           # INFO
    |               |           KeysHandler.java           # KEYS pattern
    |               |           LpushHandler.java          # LPUSH / RPUSH / LPOP / RPOP / LRANGE
    |               |           PingHandler.java           # PING → PONG
    |               |           SaddHandler.java           # SADD / SMEMBERS / SISMEMBER
    |               |           SaveHandler.java           # SAVE (trigger RDB snapshot)
    |               |           SetHandler.java            # SET key value [EX|PX|NX|XX]
    |               |
    |               +---config
    |               |       ServerConfig.java              # Port, persistence settings, etc.
    |               |
    |               +---persistence
    |               |       RdbLoader.java                 # Deserialize RDB file → DataStore
    |               |       RdbSaver.java                  # Serialize DataStore → RDB binary file
    |               |
    |               +---protocol
    |               |       RespDecoder.java               # Parses RESP bytes → RespObject
    |               |       RespEncoder.java               # Serializes RespObject → bytes
    |               |       RespObject.java                # ADT: SimpleString | Error | Integer | BulkString | Array
    |               |
    |               +---pubsub
    |               |       PublishHandler.java            # PUBLISH channel message
    |               |       PubSubManager.java             # Channel subscriptions & message fan-out
    |               |       SubscribeHandler.java          # SUBSCRIBE channel [channel ...]
    |               |
    |               +---server
    |               |       ClientHandler.java             # Per-connection read/write handler
    |               |       RexRedisServer.java            # NIO event loop, accepts connections
    |               |
    |               \---store
    |                       DataStore.java                 # Central key-value store
    |                       ExpiryManager.java             # TTL tracking & eviction
    |                       RedisValue.java                # Wrapper: type tag + underlying value
    |
    \---test
        \---java
            \---com
                \---rexredis
                    +---command
                    |   \---handlers
                    |           PingHandlerTest.java
                    |           SetGetHandlerTest.java
                    |
                    +---integration
                    |       RexRedisIntegrationTest.java
                    |
                    +---protocol
                    |       RespDecoderTest.java
                    |       RespEncoderTest.java
                    |
                    \---store
                            DataStoreTest.java
                            ExpiryManagerTest.java
```

---

## Implementation Roadmap

### Phase 1 — TCP Server & RESP Protocol

**Goal:** Accept TCP connections and speak the Redis wire protocol.

#### 1.1 The RESP Protocol

Redis clients and servers communicate using **RESP** (REdis Serialization Protocol). It's a simple text-based protocol where every message is one of five types:

| Type | Prefix | Example | Meaning |
|---|---|---|---|
| **Simple String** | `+` | `+OK\r\n` | Success response |
| **Error** | `-` | `-ERR unknown command\r\n` | Error response |
| **Integer** | `:` | `:1000\r\n` | Numeric response |
| **Bulk String** | `$` | `$5\r\nhello\r\n` | Binary-safe string (length-prefixed) |
| **Array** | `*` | `*2\r\n$3\r\nGET\r\n$4\r\nname\r\n` | Ordered collection |

**Null** is represented as `$-1\r\n` (null bulk string).

Every command from a client arrives as a **RESP Array of Bulk Strings**. For example, `SET name rex` is sent as:

```
*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$3\r\nrex\r\n
```

**What to build:**
- `RespObject` — A sealed interface (or enum-based ADT) with variants for each RESP type.
- `RespDecoder` — Reads bytes from a `ByteBuffer`, parses them into `RespObject` instances. Must handle partial reads (the full message may not arrive in one `read()` call).
- `RespEncoder` — Converts a `RespObject` back to bytes for sending to clients.

#### 1.2 NIO Event Loop

Real Redis uses a **single-threaded event loop** (via `epoll`/`kqueue`). Java's equivalent is **`java.nio`**:

```java
Selector selector = Selector.open();
ServerSocketChannel server = ServerSocketChannel.open();
server.bind(new InetSocketAddress(6379));
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();         // blocks until I/O is ready
    Set<SelectionKey> keys = selector.selectedKeys();
    for (SelectionKey key : keys) {
        if (key.isAcceptable())  handleAccept(key);
        if (key.isReadable())    handleRead(key);
        if (key.isWritable())    handleWrite(key);
    }
    keys.clear();
}
```

**Why single-threaded?** Redis avoids locks entirely — all commands execute sequentially on one thread, which makes the data store inherently thread-safe and extremely fast (no context switching, no lock contention).

**What to build:**
- `RexRedisServer` — Opens a `ServerSocketChannel`, runs the selector loop.
- `ClientHandler` — Attached to each connection's `SelectionKey`. Buffers partial reads, feeds complete messages to the decoder, writes responses back.

#### 1.3 Milestone Deliverable

After Phase 1 you should be able to:
```bash
# Start RexRedis on port 6379
java -jar rexredis.jar

# Connect with the real redis-cli
redis-cli -p 6379
> PING
PONG
> ECHO "hello world"
"hello world"
```

---

### Phase 2 — Core Commands & Data Structures

**Goal:** Implement the most-used Redis commands across all major data types.

#### 2.1 The Data Store

At its core, Redis is a **hash map** from `String` keys to typed values. In Java:

```java
public class DataStore {
    private final Map<String, RedisValue> store = new ConcurrentHashMap<>();
    // ...
}
```

Each `RedisValue` wraps the actual data and carries a **type tag**:

```java
public class RedisValue {
    enum Type { STRING, LIST, SET, HASH }
    private Type type;
    private Object value;  // String, LinkedList<String>, Set<String>, Map<String,String>
}
```

> **Type safety:** Before executing a command, always verify the value's type matches what the command expects. Return a `WRONGTYPE` error otherwise — just like real Redis.

#### 2.2 String Commands

| Command | Signature | Description |
|---|---|---|
| `SET` | `SET key value [EX seconds] [PX ms] [NX\|XX]` | Set a key. `NX` = only if not exists, `XX` = only if exists. |
| `GET` | `GET key` | Get the value of a key. Returns nil if missing. |
| `INCR` | `INCR key` | Increment the integer value. Creates key with 0 if missing. |
| `DECR` | `DECR key` | Decrement the integer value. |
| `MGET` | `MGET key [key ...]` | Get multiple keys at once. |
| `MSET` | `MSET key value [key value ...]` | Set multiple keys. |
| `APPEND` | `APPEND key value` | Append to an existing string. |
| `STRLEN` | `STRLEN key` | Return length of the string stored at key. |

#### 2.3 List Commands

Internally backed by a `LinkedList<String>`:

| Command | Description |
|---|---|
| `LPUSH key value [value ...]` | Prepend values to the head |
| `RPUSH key value [value ...]` | Append values to the tail |
| `LPOP key` | Remove and return the head element |
| `RPOP key` | Remove and return the tail element |
| `LRANGE key start stop` | Return a range of elements (0-indexed, inclusive) |
| `LLEN key` | Return the length of the list |

#### 2.4 Set Commands

Backed by a `HashSet<String>`:

| Command | Description |
|---|---|
| `SADD key member [member ...]` | Add members to the set |
| `SREM key member [member ...]` | Remove members |
| `SMEMBERS key` | Return all members |
| `SISMEMBER key member` | Check membership (returns 1 or 0) |
| `SCARD key` | Return the cardinality (size) |

#### 2.5 Hash Commands

Backed by a `HashMap<String, String>`:

| Command | Description |
|---|---|
| `HSET key field value [field value ...]` | Set field(s) in the hash |
| `HGET key field` | Get a single field's value |
| `HGETALL key` | Get all field-value pairs |
| `HDEL key field [field ...]` | Delete field(s) |
| `HEXISTS key field` | Check if a field exists |
| `HLEN key` | Return the number of fields |

#### 2.6 Key Commands

| Command | Description |
|---|---|
| `DEL key [key ...]` | Delete key(s). Returns count of deleted keys. |
| `EXISTS key [key ...]` | Check existence. Returns count of existing keys. |
| `TYPE key` | Return the type of the value at key. |
| `KEYS pattern` | Find all keys matching a glob pattern. |
| `RENAME key newkey` | Rename a key. |
| `DBSIZE` | Return the number of keys in the store. |
| `FLUSHDB` | Delete all keys. |

#### 2.7 The Command Router

```java
public class CommandRouter {
    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public CommandRouter() {
        register("PING",    new PingHandler());
        register("SET",     new SetHandler());
        register("GET",     new GetHandler());
        // ... register all handlers
    }

    public RespObject dispatch(Command cmd, DataStore store) {
        CommandHandler handler = handlers.get(cmd.name().toUpperCase());
        if (handler == null) {
            return RespObject.error("ERR unknown command '" + cmd.name() + "'");
        }
        return handler.handle(cmd, store);
    }
}
```

Each handler implements:
```java
public interface CommandHandler {
    RespObject handle(Command cmd, DataStore store);
}
```

---

### Phase 3 — TTL & Expiry

**Goal:** Support key expiration with `EXPIRE`, `TTL`, `PEXPIRE`, `PTTL`, and the `EX`/`PX` options in `SET`.

#### How Redis Handles Expiry

Redis uses **two strategies** in combination:

1. **Lazy expiration** — When a key is accessed (GET, SET, etc.), check if it has expired. If yes, delete it and return nil. This is cheap and catches most cases.

2. **Active expiration** — A periodic background task that samples random keys with TTLs and deletes expired ones. This prevents memory leaks from keys that are set-and-forgotten.

#### Implementation

```java
public class ExpiryManager {
    // key → absolute expiry time in milliseconds
    private final Map<String, Long> expiries = new ConcurrentHashMap<>();

    public void setExpiry(String key, long ttlMillis) {
        expiries.put(key, System.currentTimeMillis() + ttlMillis);
    }

    public boolean isExpired(String key) {
        Long expiry = expiries.get(key);
        return expiry != null && System.currentTimeMillis() > expiry;
    }

    public long ttlMillis(String key) {
        Long expiry = expiries.get(key);
        if (expiry == null) return -1;  // no expiry set
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? remaining : -2;  // -2 = expired / doesn't exist
    }

    /** Called periodically from the event loop */
    public void activeExpiryCycle(DataStore store) {
        // Sample 20 random keys with TTLs, delete expired ones
        // If > 25% were expired, run again immediately
    }
}
```

**Integrate into the event loop:** After each `selector.select()` call (or on a timer), invoke the active expiry cycle.

---

### Phase 4 — Persistence (RDB Snapshots)

**Goal:** Save the in-memory state to disk so data survives restarts.

#### What is RDB?

RDB (Redis Database) is a **point-in-time snapshot** of the entire dataset. It's a compact binary file. On startup, Redis reads the RDB file to restore state.

#### Simplified RDB Format

For this project, you can implement a **simplified binary format**:

```
[MAGIC: "REXREDIS"] [VERSION: 1 byte]
[NUM_KEYS: 4 bytes]
For each key:
  [TYPE: 1 byte]           // 0=STRING, 1=LIST, 2=SET, 3=HASH
  [HAS_EXPIRY: 1 byte]     // 0 or 1
  [EXPIRY_MS: 8 bytes]     // only if HAS_EXPIRY=1
  [KEY_LEN: 4 bytes] [KEY: N bytes]
  [VALUE]                   // encoding depends on TYPE
    STRING: [VAL_LEN: 4 bytes] [VAL: N bytes]
    LIST:   [COUNT: 4 bytes] [LEN+DATA for each element]
    SET:    [COUNT: 4 bytes] [LEN+DATA for each element]
    HASH:   [COUNT: 4 bytes] [LEN+KEY+LEN+VAL for each pair]
[EOF: 0xFF]
```

#### When to Save

| Strategy | Trigger | Example |
|---|---|---|
| **Manual** | `SAVE` command | User explicitly triggers |
| **Periodic** | Timer-based | Every 60 seconds if ≥ 100 keys changed |
| **On Shutdown** | Graceful shutdown hook | `Runtime.addShutdownHook(...)` |

#### What to Build

- `RdbSaver` — Iterates the `DataStore`, writes each key-value pair in the binary format.
- `RdbLoader` — Reads the binary file on startup, populates the `DataStore`. Skips keys whose stored expiry time is already in the past.

---

### Phase 5 — Pub/Sub

**Goal:** Let clients subscribe to channels and receive messages in real time.

#### How Pub/Sub Works

```
Client A                    Server                   Client B
   │                          │                          │
   │── SUBSCRIBE news ──────▶ │                          │
   │                          │                          │
   │                          │ ◀── PUBLISH news "hi" ── │
   │                          │                          │
   │◀── message news "hi" ── │                          │
```

- **`SUBSCRIBE channel`** — Client enters "subscription mode." It can no longer send regular commands (only `SUBSCRIBE`, `UNSUBSCRIBE`, `PING`).
- **`PUBLISH channel message`** — Fan-out the message to all subscribers. Returns the number of clients that received it.
- **`UNSUBSCRIBE [channel]`** — Leave one or all channels.

#### Implementation

```java
public class PubSubManager {
    // channel → set of subscribed client handlers
    private final Map<String, Set<ClientHandler>> subscriptions = new ConcurrentHashMap<>();

    public void subscribe(String channel, ClientHandler client) { ... }
    public int publish(String channel, String message) { ... }
    public void unsubscribe(String channel, ClientHandler client) { ... }
}
```

---

### Phase 6 — Advanced Features (Stretch Goals)

Once the core is solid, consider these extensions:

| Feature | Description |
|---|---|
| **AOF (Append-Only File)** | Log every write command to disk. Replay on startup for durability. |
| **Transactions** | `MULTI` / `EXEC` / `DISCARD` — queue commands and execute atomically. |
| **Lua Scripting** | `EVAL` — execute Lua scripts server-side (use GraalVM or Luaj). |
| **Replication** | Leader-follower replication with `REPLCONF` and `PSYNC`. |
| **Cluster** | Sharding keys across multiple RexRedis nodes via hash slots. |
| **Sorted Sets** | `ZADD`, `ZRANGE`, `ZRANK` — backed by a **skip list** + hash map. |
| **Streams** | `XADD`, `XREAD` — append-only log data structure. |
| **Eviction Policies** | LRU, LFU, random eviction when memory limit is reached. |
| **AUTH** | `AUTH password` — simple password authentication. |
| **SELECT** | Multiple logical databases (0–15). |

---

## Key Concepts Explained

### Why Single-Threaded?

Redis processes all commands on a **single thread**. This might sound slow, but it's actually a deliberate design choice:

- **No locks needed** — The data store is never accessed concurrently, so operations are naturally atomic.
- **No context switching** — The CPU stays focused on processing commands.
- **Bottleneck is I/O, not CPU** — For most workloads, network latency dominates. A single thread can handle 100K+ ops/sec.
- **Simplicity** — Reasoning about single-threaded code is dramatically easier.

Java NIO's `Selector` lets one thread multiplex thousands of connections without blocking.

### Java NIO vs. Netty

| Approach | Pros | Cons |
|---|---|---|
| **Raw `java.nio`** | No dependencies, full control, educational | More boilerplate, manual buffer management |
| **Netty** | Production-grade, battle-tested, pipeline model | Large dependency, hides the learning |

This project uses **raw `java.nio`** to maximize learning. You can always refactor to Netty later.

### RESP vs. Other Protocols

RESP was chosen by Redis because it is:
- **Simple to implement** — Just prefix bytes and `\r\n` delimiters.
- **Human-readable** — You can debug with `telnet`.
- **Fast to parse** — Length-prefixed strings avoid scanning for delimiters.

---

## How to Build & Run

### Prerequisites

- **Java 21+** (we use modern Java features like sealed interfaces, pattern matching)
- **Maven 3.9+**

### Build

```bash
mvn clean package
```

### Run

```bash
# Start on default port 6379
java -jar target/rexredis-1.0.0-SNAPSHOT.jar

# Start on a custom port
java -jar target/rexredis-1.0.0-SNAPSHOT.jar --port 6380
```

### Connect

```bash
# Use the official redis-cli (install via redis-tools)
redis-cli -p 6379

# Or use telnet for raw RESP
telnet localhost 6379
```

---

## Testing

### Unit Tests

```bash
mvn test
```

Tests are organized by layer:

- **Protocol tests** — Feed raw RESP bytes into the decoder, assert correct `RespObject` output. Feed `RespObject` into the encoder, assert correct bytes.
- **Command tests** — Create a `DataStore`, invoke handlers directly, assert correct responses.
- **Expiry tests** — Set keys with TTLs, advance time (use a `Clock` abstraction), verify expiration.

### Integration Tests

Full end-to-end tests that boot the server, connect a real client, send commands, and verify responses.

```bash
mvn verify -Pintegration
```

### Manual Testing with redis-cli

The best way to test is using the official `redis-cli`. If your RESP implementation is correct, `redis-cli` will work out of the box since it speaks the same protocol.

---

## References

- [Redis Protocol Specification (RESP)](https://redis.io/docs/reference/protocol-spec/)
- [Redis Commands Reference](https://redis.io/commands/)
- [Build Your Own Redis (CodeCrafters)](https://codecrafters.io/challenges/redis)
- [Java NIO Tutorial](https://docs.oracle.com/javase/tutorial/essential/io/fileio.html)
- [Redis Internals — RDB File Format](https://rdb.fnordig.de/file_format.html)

---

## License

MIT
