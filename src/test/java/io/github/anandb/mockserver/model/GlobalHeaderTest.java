package io.github.anandb.mockserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalHeader Tests")
class GlobalHeaderTest {

    @Test
    void constructorSetsNameAndValue() {
        GlobalHeader header = new GlobalHeader("X-Custom", "test-value");
        assertEquals("X-Custom", header.getName());
        assertEquals("test-value", header.getValue());
    }

    @Test
    void defaultConstructorWorks() {
        GlobalHeader header = new GlobalHeader();
        assertNull(header.getName());
        assertNull(header.getValue());
    }

    @Test
    void settersWork() {
        GlobalHeader header = new GlobalHeader();
        header.setName("X-New");
        header.setValue("new-value");
        assertEquals("X-New", header.getName());
        assertEquals("new-value", header.getValue());
    }
}
