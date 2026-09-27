package com.rexredis.persistence;

import com.rexredis.store.DataStore;
import com.rexredis.store.RedisValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Deserializes an RDB snapshot file back into a DataStore.
 */
public class RdbLoader {

    /**
     * Loads data from an RDB file into the data store.
     * Skips keys whose stored expiry time is already in the past.
     *
     * @param filePath the RDB file to load
     * @param store    the data store to populate
     * @throws IOException if reading fails or file is corrupted
     */
    public void load(String filePath, DataStore store) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("RDB file not found: " + filePath);
        }

        try (InputStream fis = Files.newInputStream(path);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis)) {

            readSnapshot(dis, store);
        }
    }

    /**
     * Reads snapshot directly from a DataInputStream.
     */
    public void readSnapshot(DataInputStream dis, DataStore store) throws IOException {
        // 1. Magic check
        byte[] magic = new byte[RdbSaver.MAGIC.length];
        dis.readFully(magic);
        if (!Arrays.equals(magic, RdbSaver.MAGIC)) {
            throw new IOException("Invalid RDB magic header");
        }

        // 2. Version check
        byte version = dis.readByte();
        if (version != RdbSaver.VERSION) {
            throw new IOException("Unsupported RDB version: " + version);
        }

        // 3. Number of keys
        int numKeys = dis.readInt();
        long now = System.currentTimeMillis();

        for (int i = 0; i < numKeys; i++) {
            byte typeByte = dis.readByte();
            byte hasExpiryByte = dis.readByte();
            Long expiryMs = null;
            if (hasExpiryByte == 1) {
                expiryMs = dis.readLong();
            }

            String key = readString(dis);
            RedisValue val = switch (typeByte) {
                case RdbSaver.TYPE_STRING -> RedisValue.string(readString(dis));
                case RdbSaver.TYPE_LIST -> {
                    int count = dis.readInt();
                    List<String> list = new ArrayList<>(count);
                    for (int j = 0; j < count; j++) {
                        list.add(readString(dis));
                    }
                    yield RedisValue.list(list);
                }
                case RdbSaver.TYPE_SET -> {
                    int count = dis.readInt();
                    Set<String> set = new LinkedHashSet<>(count);
                    for (int j = 0; j < count; j++) {
                        set.add(readString(dis));
                    }
                    yield RedisValue.set(set);
                }
                case RdbSaver.TYPE_HASH -> {
                    int count = dis.readInt();
                    Map<String, String> map = new LinkedHashMap<>(count);
                    for (int j = 0; j < count; j++) {
                        String field = readString(dis);
                        String value = readString(dis);
                        map.put(field, value);
                    }
                    yield RedisValue.hash(map);
                }
                default -> throw new IOException("Unknown RDB value type: " + typeByte);
            };

            // If the key has an expired TTL, ignore it
            if (expiryMs != null && now > expiryMs) {
                continue;
            }

            store.set(key, val);
            if (expiryMs != null) {
                store.getExpiryManager().setAbsoluteExpiry(key, expiryMs);
            }
        }

        // EOF marker
        byte eof = dis.readByte();
        if (eof != RdbSaver.EOF_MARKER) {
            throw new IOException("Corrupted RDB file: missing EOF marker (found: " + eof + ")");
        }
    }

    private String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length < 0) {
            throw new IOException("Invalid string length in RDB: " + length);
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
