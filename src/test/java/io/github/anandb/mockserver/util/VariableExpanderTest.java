package io.github.anandb.mockserver.util;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VariableExpanderTest {

    @Test
    void testSimpleExpansion() {
        Map<String, String> vars = Map.of("VAR", "value");
        assertEquals("value", VariableExpander.expand("@{VAR}", vars));
    }

    @Test
    void testExpansionInText() {
        Map<String, String> vars = Map.of("VAR", "value");
        assertEquals("Hello value World", VariableExpander.expand("Hello @{VAR} World", vars));
    }

    @Test
    void testMultipleExpansions() {
        Map<String, String> vars = Map.of("VAR1", "v1", "VAR2", "v2");
        assertEquals("v1 and v2", VariableExpander.expand("@{VAR1} and @{VAR2}", vars));
    }

    @Test
    void testDefaultValueWhenMissing() {
        Map<String, String> vars = Map.of();
        assertEquals("default", VariableExpander.expand("@{VAR:-default}", vars));
    }

    @Test
    void testVarOverDefaultValue() {
        Map<String, String> vars = Map.of("VAR", "value");
        assertEquals("value", VariableExpander.expand("@{VAR:-default}", vars));
    }

    @Test
    void testEmptyDefaultValue() {
        Map<String, String> vars = Map.of();
        assertThrows(IllegalArgumentException.class, () -> VariableExpander.expand("@{VAR}", vars));
    }

    @Test
    void testErrorWhenMissingAndNoDefault() {
        Map<String, String> vars = Map.of();
        assertThrows(IllegalArgumentException.class, () -> VariableExpander.expand("@{VAR}", vars));
    }

    @Test
    void testNestedLikeSyntax() {
        Map<String, String> vars = Map.of("VAR", "value");
        assertEquals("value", VariableExpander.expand("@{VAR:-other}", vars));
    }

    @Test
    void testSpecialCharactersInDefault() {
        Map<String, String> vars = Map.of();
        assertEquals("http://localhost:8080", VariableExpander.expand("@{URL:-http://localhost:8080}", vars));
    }

    @Test
    void testNullInput() {
        assertNull(VariableExpander.expand(null));
    }

    @Test
    void testNoVariablesInText() {
        assertEquals("Plain text", VariableExpander.expand("Plain text", Map.of()));
    }
}
