package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EnhancedExpectation Tests")
class EnhancedExpectationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void getHttpRequestReturnsNullWhenNull() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertNull(config.getHttpRequest());
    }

    @Test
    void getHttpResponseReturnsNullWhenNull() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertNull(config.getHttpResponse());
    }

    @Test
    void isSseReturnsFalseWhenNotSet() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertFalse(config.isSse());
    }

    @Test
    void isSseReturnsTrueWhenEnabledWithMessages() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode()
                        .set("messages", mapper.createArrayNode().add("msg1")))
                .sse(true)
                .build();
        assertTrue(config.isSse());
    }

    @Test
    void isSseReturnsFalseWhenNoMessages() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode())
                .sse(true)
                .build();
        assertFalse(config.isSse());
    }

    @Test
    void getMessagesReturnsEmptyWhenHttpResponseNull() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertTrue(config.getMessages().isEmpty());
    }

    @Test
    void getMessagesReturnsEmptyWhenNoMessagesField() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode().put("statusCode", 200))
                .build();
        assertTrue(config.getMessages().isEmpty());
    }

    @Test
    void getMessagesReturnsListWhenPresent() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode()
                        .set("messages", mapper.createArrayNode().add("msg1").add("msg2")))
                .build();
        List<String> messages = config.getMessages();
        assertEquals(2, messages.size());
        assertEquals("msg1", messages.get(0));
        assertEquals("msg2", messages.get(1));
    }

    @Test
    void isFileResponseReturnsFalseWhenNoFile() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertFalse(config.isFileResponse());
    }

    @Test
    void isFileResponseReturnsTrueWhenFilePresent() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode().put("file", "/data/file.pdf"))
                .build();
        assertTrue(config.isFileResponse());
    }

    @Test
    void getFileReturnsNullWhenNotPresent() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertNull(config.getFile());
    }

    @Test
    void getFileReturnsPathWhenPresent() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode().put("file", "/data/file.pdf"))
                .build();
        assertEquals("/data/file.pdf", config.getFile());
    }

    @Test
    void builderBuildsCompleteObject() {
        JsonNode req = mapper.createObjectNode().put("method", "GET").put("path", "/test");
        ObjectNode res = mapper.createObjectNode().put("statusCode", 200);
        res.set("messages", mapper.createArrayNode().add("event"));
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpRequest(req)
                .httpResponse(res)
                .sse(true)
                .interval(1000)
                .build();

        assertNotNull(config.getHttpRequest());
        assertNotNull(config.getHttpResponse());
        assertTrue(config.isSse());
    }
}
