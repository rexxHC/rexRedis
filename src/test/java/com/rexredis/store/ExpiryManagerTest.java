package com.rexredis.store;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the ExpiryManager.
 */
class ExpiryManagerTest {

    // TODO: Test cases:
    //  - Set expiry, check isExpired before/after TTL
    //  - ttlMillis returns correct remaining time
    //  - ttlMillis returns -1 when no expiry set
    //  - ttlMillis returns -2 when expired
    //  - removeExpiry clears the TTL
    //  - activeExpiryCycle removes expired keys

    @Test
    void placeholder() {
        assertThat(true).isTrue();
    }
}
