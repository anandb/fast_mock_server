package io.github.anandb.mockserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TunnelConfig Tests")
class TunnelConfigTest {

    @Test
    void constructorSetsFields() {
        TunnelConfig config = new TunnelConfig("namespace", "pod-prefix", 8080);
        assertEquals("namespace", config.getNamespace());
        assertEquals("pod-prefix", config.getPodPrefix());
        assertEquals(8080, config.getPodPort());
    }

    @Test
    void defaultConstructorWorks() {
        TunnelConfig config = new TunnelConfig();
        assertNull(config.getNamespace());
        assertNull(config.getPodPrefix());
        assertNull(config.getPodPort());
    }
}
