# RexRedis

A Redis clone built from scratch in Java using raw NIO.

## Features

- **RESP Protocol** — Full parser/serializer for the Redis wire protocol
- **Single-threaded event loop** — Java NIO `Selector`, no locks needed
- **Data types** — Strings, Lists, Sets, Hashes
- **25+ commands** — SET, GET, INCR, LPUSH, SADD, HSET, DEL, KEYS, EXPIRE, TTL, PUBLISH, SUBSCRIBE, etc.
- **TTL & Expiry** — Lazy + active eviction (Redis-style randomized sampling)
- **RDB Persistence** — Binary snapshots with atomic save, auto-load on startup
- **Pub/Sub** — Channel subscriptions with cross-client message fan-out

## Quick Start

```bash
# Build
mvn clean package

# Run (default port 6379)
java -jar target/rexredis-1.0.0-SNAPSHOT.jar

# Custom port / disable persistence
java -jar target/rexredis-1.0.0-SNAPSHOT.jar --port 6380
java -jar target/rexredis-1.0.0-SNAPSHOT.jar --no-persistence

# Connect with redis-cli
redis-cli -p 6379
```

Requires **Java 21+** and **Maven 3.9+**.

## Architecture

```
Client → NIO Event Loop → RESP Decoder → Command Router → Handler → DataStore
                                                                  ↕
                                                          ExpiryManager / RDB
```

Single-threaded like real Redis — all commands execute sequentially on one thread. Pub/Sub fan-out uses synchronized writes for cross-client delivery.

## Project Structure

```
src/main/java/com/rexredis/
├── RexRedisApplication.java        # Entry point
├── server/
│   ├── RexRedisServer.java         # NIO event loop
│   └── ClientHandler.java          # Per-connection I/O
├── protocol/
│   ├── RespObject.java             # RESP type ADT
│   ├── RespDecoder.java            # Bytes → RespObject
│   └── RespEncoder.java            # RespObject → bytes
├── command/
│   ├── Command.java                # Parsed command record
│   ├── CommandRouter.java          # Name → handler dispatch
│   └── handlers/                   # One handler per command group
├── store/
│   ├── DataStore.java              # Key-value store
│   ├── RedisValue.java             # Typed value wrapper
│   └── ExpiryManager.java          # TTL tracking & eviction
├── persistence/
│   ├── RdbSaver.java               # Snapshot serializer
│   └── RdbLoader.java              # Snapshot deserializer
├── pubsub/
│   ├── PubSubManager.java          # Subscription & fan-out
│   ├── SubscribeHandler.java       # SUBSCRIBE/UNSUBSCRIBE
│   └── PublishHandler.java         # PUBLISH
└── config/
    └── ServerConfig.java           # CLI args & settings
```

## Supported Commands

| Category | Commands |
|----------|----------|
| **Strings** | `SET` `GET` `INCR` `DECR` `INCRBY` `DECRBY` |
| **Lists** | `LPUSH` `RPUSH` `LPOP` `RPOP` `LLEN` `LRANGE` |
| **Sets** | `SADD` `SREM` `SMEMBERS` `SISMEMBER` `SCARD` |
| **Hashes** | `HSET` `HGET` `HGETALL` `HDEL` `HEXISTS` `HLEN` |
| **Keys** | `DEL` `EXISTS` `TYPE` `KEYS` `DBSIZE` `FLUSHDB` |
| **Expiry** | `EXPIRE` `PEXPIRE` `EXPIREAT` `PEXPIREAT` `TTL` `PTTL` `PERSIST` |
| **Pub/Sub** | `SUBSCRIBE` `UNSUBSCRIBE` `PUBLISH` |
| **Server** | `PING` `ECHO` `INFO` `SAVE` `BGSAVE` |

## Testing

```bash
# Unit tests (63 tests)
mvn test

# Integration tests (13 tests, real TCP + Jedis client)
mvn test -Dtest="*IntegrationTest"
```

## License

MIT
