package io.github.anandb.mockserver.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.anandb.mockserver.model.EnhancedExpectation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SSEResponseStrategy Tests")
class SSEResponseStrategyTest {

    private SSEResponseStrategy strategy;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        strategy = new SSEResponseStrategy();
        mapper = new ObjectMapper();
    }

    @Test
    void handleCreatesSSEResponseWithMessages() {
        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200);
        httpResponse.set("messages", mapper.createArrayNode().add("event1").add("event2"));
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .sse(true)
                .build();

        HttpResponse result = strategy.handle(HttpRequest.request(), config, Map.of());

        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertEquals("text/event-stream", result.getFirstHeader("Content-Type"));
        assertEquals("no-cache", result.getFirstHeader("Cache-Control"));
        assertTrue(result.getBodyAsString().contains("data: event1"));
        assertTrue(result.getBodyAsString().contains("data: event2"));
    }

    @Test
    void handleDefaultsTo200WhenNoHttpResponse() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .sse(true)
                .build();

        HttpResponse result = strategy.handle(HttpRequest.request(), config, Map.of());

        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void handleIncludesSseHeaders() {
        ObjectNode httpResponse = mapper.createObjectNode();
        httpResponse.set("messages", mapper.createArrayNode().add("data"));
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .sse(true)
                .build();

        HttpResponse result = strategy.handle(HttpRequest.request(), config, Map.of());

        assertEquals("text/event-stream", result.getFirstHeader("Content-Type"));
        assertEquals("no-cache", result.getFirstHeader("Cache-Control"));
        assertEquals("keep-alive", result.getFirstHeader("Connection"));
    }

    @Test
    void supportsReturnsTrueWhenSseEnabled() {
        ObjectNode httpResponse = mapper.createObjectNode();
        httpResponse.set("messages", mapper.createArrayNode().add("msg"));
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .sse(true)
                .build();

        assertTrue(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseWhenSseNotEnabled() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .sse(false)
                .build();

        assertFalse(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseWhenSseButNoMessages() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode())
                .sse(true)
                .build();

        assertFalse(strategy.supports(config));
    }

    @Test
    void getPriorityIs20() {
        assertEquals(20, strategy.getPriority());
    }
}
