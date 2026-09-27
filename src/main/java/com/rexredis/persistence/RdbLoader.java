package com.rexredis.persistence;

import com.rexredis.store.DataStore;

import java.io.IOException;

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
     * @throws IOException if reading fails
     */
    public void load(String filePath, DataStore store) throws IOException {
        // TODO: Implement RDB binary format deserialization
        //  1. Read and validate magic header + version
        //  2. Read number of keys
        //  3. For each key: read type, expiry, key, value
        //  4. Skip keys with expired TTLs
        //  5. Verify EOF marker
    }
}
