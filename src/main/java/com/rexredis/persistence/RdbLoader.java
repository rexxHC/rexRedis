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

    /** Maximum string length from RDB: 64 MB */
    private static final int MAX_STRING_LENGTH = 64 * 1024 * 1024;
    /** Maximum collection size from RDB */
    private static final int MAX_COLLECTION_SIZE = 10_000_000;

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
             java.util.zip.CheckedInputStream cis = new java.util.zip.CheckedInputStream(bis, new java.util.zip.CRC32());
             DataInputStream dis = new DataInputStream(cis)) {

            readSnapshot(dis, store);
            long calculatedCrc = cis.getChecksum().getValue();
            long expectedCrc = dis.readLong();
            if (calculatedCrc != expectedCrc) {
                throw new IOException("RDB file corrupted (CRC mismatch)");
            }
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

        // 3. Load into a temporary store for atomic population
        DataStore tempStore = new DataStore();
        int numKeys = dis.readInt();
        if (numKeys < 0) {
            throw new IOException("Invalid key count in RDB: " + numKeys);
        }
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
                    if (count < 0 || count > MAX_COLLECTION_SIZE) {
                        throw new IOException("Invalid list count in RDB: " + count);
                    }
                    List<String> list = new ArrayList<>(Math.min(count, 1024));
                    for (int j = 0; j < count; j++) {
                        list.add(readString(dis));
                    }
                    yield RedisValue.list(list);
                }
                case RdbSaver.TYPE_SET -> {
                    int count = dis.readInt();
                    if (count < 0 || count > MAX_COLLECTION_SIZE) {
                        throw new IOException("Invalid set count in RDB: " + count);
                    }
                    Set<String> set = new LinkedHashSet<>(Math.min(count, 1024));
                    for (int j = 0; j < count; j++) {
                        set.add(readString(dis));
                    }
                    yield RedisValue.set(set);
                }
                case RdbSaver.TYPE_HASH -> {
                    int count = dis.readInt();
                    if (count < 0 || count > MAX_COLLECTION_SIZE) {
                        throw new IOException("Invalid hash count in RDB: " + count);
                    }
                    Map<String, String> map = new LinkedHashMap<>(Math.min(count, 1024));
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

            tempStore.set(key, val);
            if (expiryMs != null) {
                tempStore.getExpiryManager().setAbsoluteExpiry(key, expiryMs);
            }
        }

        // EOF marker — validates the file is complete
        byte eof = dis.readByte();
        if (eof != RdbSaver.EOF_MARKER) {
            throw new IOException("Corrupted RDB file: missing EOF marker (found: " + eof + ")");
        }

        // Only populate the real store after successful validation
        for (Map.Entry<String, RedisValue> entry : tempStore.getAll().entrySet()) {
            store.set(entry.getKey(), entry.getValue());
            Long expiry = tempStore.getExpiryManager().getExpiry(entry.getKey());
            if (expiry != null) {
                store.getExpiryManager().setAbsoluteExpiry(entry.getKey(), expiry);
            }
        }
    }

    private String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length < 0 || length > MAX_STRING_LENGTH) {
            throw new IOException("Invalid or excessive string length in RDB: " + length);
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
