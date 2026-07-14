package io.github.anandb.mockserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MockRequestDTO Tests")
class MockRequestDTOTest {

    @Test
    void constructorSetsMethodAndPath() {
        MockRequestDTO dto = new MockRequestDTO("POST", "/api/users");
        assertEquals("POST", dto.getMethod());
        assertEquals("/api/users", dto.getPath());
        assertNull(dto.getBody());
    }

    @Test
    void defaultConstructorCreatesEmptyDto() {
        MockRequestDTO dto = new MockRequestDTO();
        assertNull(dto.getMethod());
        assertNull(dto.getPath());
        assertNull(dto.getBody());
    }

    @Test
    void settersAndGetters() {
        MockRequestDTO dto = new MockRequestDTO();
        dto.setMethod("DELETE");
        dto.setPath("/api/items/{id}");
        dto.setBody(Map.of("key", "value"));

        assertEquals("DELETE", dto.getMethod());
        assertEquals("/api/items/{id}", dto.getPath());
        assertNotNull(dto.getBody());
    }

    @Test
    void bodyCanBeAnyObject() {
        MockRequestDTO dto = new MockRequestDTO("PUT", "/api");
        dto.setBody("plain string body");
        assertEquals("plain string body", dto.getBody());

        dto.setBody(42);
        assertEquals(42, dto.getBody());
    }

    @Test
    void testEqualsAndHashCode() {
        MockRequestDTO a = new MockRequestDTO("GET", "/api");
        MockRequestDTO b = new MockRequestDTO("GET", "/api");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testToString() {
        MockRequestDTO dto = new MockRequestDTO("GET", "/test");
        String str = dto.toString();
        assertTrue(str.contains("GET"));
        assertTrue(str.contains("/test"));
    }

    private static java.util.Map<String, String> Map(String key, String value) {
        return java.util.Map.of(key, value);
    }
}
