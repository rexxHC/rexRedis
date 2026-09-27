package com.rexredis.persistence;

import com.rexredis.store.DataStore;

import java.io.IOException;

/**
 * Serializes the DataStore to a binary RDB snapshot file.
 */
public class RdbSaver {

    /**
     * Saves the entire data store to the specified file path.
     *
     * @param store    the data store to snapshot
     * @param filePath the output file path
     * @throws IOException if writing fails
     */
    public void save(DataStore store, String filePath) throws IOException {
        // TODO: Implement RDB binary format serialization
        //  1. Write magic header "REXREDIS" + version byte
        //  2. Write number of keys
        //  3. For each key: type, expiry info, key, value
        //  4. Write EOF marker (0xFF)
    }
}
