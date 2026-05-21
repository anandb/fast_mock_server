package io.github.anandb.mockserver.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.anandb.mockserver.model.EnhancedExpectation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StaticResponseStrategy Tests")
class StaticResponseStrategyTest {

    private StaticResponseStrategy strategy;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        strategy = new StaticResponseStrategy();
        mapper = new ObjectMapper();
    }

    @Test
    void handleReturnsConfigResponse() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode().put("statusCode", 200).put("body", "ok"))
                .build();

        HttpResponse result = strategy.handle(HttpRequest.request(), config, Map.of());

        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void handleReturnsNullWhenNoHttpResponse() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertNull(strategy.handle(HttpRequest.request(), config, Map.of()));
    }

    @Test
    void supportsNonSseNonFile() {
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(mapper.createObjectNode())
                .sse(false)
                .build();

        assertTrue(strategy.supports(config));
    }

    @Test
    void doesNotSupportSseWithMessages() {
        ObjectNode httpResponse = mapper.createObjectNode();
        httpResponse.set("messages", mapper.createArrayNode().add("msg1"));
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .sse(true)
                .build();

        assertFalse(strategy.supports(config));
    }

    @Test
    void doesNotSupportFileResponse() {
        ObjectNode httpResponse = mapper.createObjectNode();
        httpResponse.put("file", "/path/to/file");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        assertFalse(strategy.supports(config));
    }

    @Test
    void getPriorityIsMinus100() {
        assertEquals(-100, strategy.getPriority());
    }
}
