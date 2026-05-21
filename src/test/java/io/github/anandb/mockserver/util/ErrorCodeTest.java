package io.github.anandb.mockserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCode Tests")
class ErrorCodeTest {

    @Test
    void testConstructor() {
        ErrorCode code = new ErrorCode("TEST_ERROR", "Test message");
        assertNotNull(code);
    }

    @Test
    void testToStringReturnsJson() throws JsonProcessingException {
        ErrorCode code = new ErrorCode("NOT_FOUND", "Resource not found");
        String json = code.toString();

        assertTrue(json.contains("NOT_FOUND"));
        assertTrue(json.contains("Resource not found"));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }
}
