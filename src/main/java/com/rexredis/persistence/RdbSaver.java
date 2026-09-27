package com.rexredis.persistence;

import com.rexredis.store.DataStore;
import com.rexredis.store.ExpiryManager;
import com.rexredis.store.RedisValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Serializes the DataStore to a binary RDB snapshot file.
 *
 * <p>Format:
 * <pre>
 * [MAGIC: "REXREDIS" (8 bytes)]
 * [VERSION: 1 byte (0x01)]
 * [NUM_KEYS: 4 bytes int]
 * For each key:
 *   [TYPE: 1 byte (0=STRING, 1=LIST, 2=SET, 3=HASH)]
 *   [HAS_EXPIRY: 1 byte (0 or 1)]
 *   [EXPIRY_MS: 8 bytes long (if HAS_EXPIRY=1)]
 *   [KEY_LEN: 4 bytes int] [KEY: N bytes UTF-8]
 *   [VALUE: type-dependent encoding]
 * [EOF: 1 byte (0xFF)]
 * </pre>
 */
public class RdbSaver {

    public static final byte[] MAGIC = "REXREDIS".getBytes(StandardCharsets.US_ASCII);
    public static final byte VERSION = 1;
    public static final byte EOF_MARKER = (byte) 0xFF;

    public static final byte TYPE_STRING = 0;
    public static final byte TYPE_LIST = 1;
    public static final byte TYPE_SET = 2;
    public static final byte TYPE_HASH = 3;

    /**
     * Saves the entire active data store to the specified file path atomically.
     *
     * @param store    the data store to snapshot
     * @param filePath the output file path
     * @throws IOException if writing fails
     */
    public void save(DataStore store, String filePath) throws IOException {
        Path targetPath = Paths.get(filePath);
        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Path tempPath = Paths.get(filePath + ".tmp");

        try (OutputStream fos = Files.newOutputStream(tempPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(bos)) {

            writeSnapshot(store, dos);
            dos.flush();
        }

        // Atomically replace target file
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Writes snapshot directly to an output stream (useful for tests and socket sync).
     */
    public void writeSnapshot(DataStore store, DataOutputStream dos) throws IOException {
        // 1. Magic + Version
        dos.write(MAGIC);
        dos.writeByte(VERSION);

        // Gather active, non-expired keys
        Set<String> activeKeys = store.keys();
        ExpiryManager expiryManager = store.getExpiryManager();

        dos.writeInt(activeKeys.size());

        for (String key : activeKeys) {
            RedisValue val = store.get(key);
            if (val == null) {
                continue;
            }

            // Type
            byte typeByte = switch (val.getType()) {
                case STRING -> TYPE_STRING;
                case LIST -> TYPE_LIST;
                case SET -> TYPE_SET;
                case HASH -> TYPE_HASH;
            };
            dos.writeByte(typeByte);

            // Expiry
            boolean hasExpiry = expiryManager.hasExpiry(key);
            dos.writeByte(hasExpiry ? 1 : 0);
            if (hasExpiry) {
                dos.writeLong(expiryManager.getExpiry(key));
            }

            // Key
            writeString(dos, key);

            // Value
            switch (val.getType()) {
                case STRING -> writeString(dos, val.asString());
                case LIST -> {
                    List<String> list = val.asList();
                    dos.writeInt(list.size());
                    for (String elem : list) {
                        writeString(dos, elem);
                    }
                }
                case SET -> {
                    Set<String> set = val.asSet();
                    dos.writeInt(set.size());
                    for (String elem : set) {
                        writeString(dos, elem);
                    }
                }
                case HASH -> {
                    Map<String, String> map = val.asHash();
                    dos.writeInt(map.size());
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        writeString(dos, entry.getKey());
                        writeString(dos, entry.getValue());
                    }
                }
            }
        }

        // EOF marker
        dos.writeByte(EOF_MARKER);
    }

    private void writeString(DataOutputStream dos, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }
}
