package com.deepface.models;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelUrlsChecksumTest {

    @Test
    void checksumsCoverDefaultsAndHaveValidFormat() {
        Map<String, String> defaults = ModelUrls.defaults();
        Map<String, String> checks = ModelUrls.checksums();

        assertNotNull(checks, "checks map should not be null");

        // Each default model should have an entry in the checks map (maybe placeholder)
        for (String key : defaults.keySet()) {
            assertTrue(checks.containsKey(key), "Missing checksum entry for: " + key);
            String val = checks.get(key);
            assertNotNull(val, "Checksum value for " + key + " should not be null");
            // Must be a 64-character hex string
            assertTrue(val.matches("^[a-fA-F0-9]{64}$"), "Checksum for " + key + " is not a 64-char hex string");
        }
    }
}
