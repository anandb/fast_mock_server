package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MtlsConfig Tests")
class MtlsConfigTest {

    @Test
    void isValidReturnsTrueWhenCaPresent() {
        MtlsConfig config = new MtlsConfig("ca-cert-content", true);
        assertTrue(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenCaNull() {
        MtlsConfig config = new MtlsConfig(null, true);
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenCaBlank() {
        MtlsConfig config = new MtlsConfig("   ", true);
        assertFalse(config.isValid());
    }

    @Test
    void requireClientAuthDefaultsToTrue() {
        MtlsConfig config = new MtlsConfig();
        assertTrue(config.isRequireClientAuth());
    }
}
