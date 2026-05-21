package io.github.anandb.mockserver.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.anandb.mockserver.model.EnhancedExpectation;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("EnhancedResponseCallback Tests")
@ExtendWith(MockitoExtension.class)
class EnhancedResponseCallbackTest {

    @Mock
    private ResponseStrategy strategy1;

    @Mock
    private ResponseStrategy strategy2;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void handleDelegatesToSupportingStrategy() {
        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200);
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .sse(true)
                .build();
        httpResponse.set("messages", mapper.createArrayNode().add("event"));

        lenient().when(strategy1.supports(any())).thenReturn(false);
        lenient().when(strategy1.getPriority()).thenReturn(0);
        when(strategy2.supports(any())).thenReturn(true);
        when(strategy2.getPriority()).thenReturn(10);
        when(strategy2.handle(any(), any(), anyMap()))
                .thenReturn(HttpResponse.response().withStatusCode(200).withBody("handled"));

        EnhancedResponseCallback callback = new EnhancedResponseCallback(
                config, List.of(), List.of(strategy1, strategy2), "/test"
        );

        HttpResponse result = callback.handle(HttpRequest.request().withPath("/test"));

        assertEquals(200, result.getStatusCode());
        assertEquals("handled", result.getBodyAsString());
        verify(strategy2).handle(any(), eq(config), anyMap());
    }

    @Test
    void handleReturns500WhenNoStrategyFound() {
        EnhancedExpectation config = EnhancedExpectation.builder().build();

        lenient().when(strategy1.supports(any())).thenReturn(false);
        lenient().when(strategy1.getPriority()).thenReturn(0);

        EnhancedResponseCallback callback = new EnhancedResponseCallback(
                config, List.of(), List.of(strategy1), "/test"
        );

        HttpResponse result = callback.handle(HttpRequest.request().withPath("/test"));

        assertEquals(500, result.getStatusCode());
    }

    @Test
    void handleReturns500WhenStrategyThrows() {
        EnhancedExpectation config = EnhancedExpectation.builder().build();

        lenient().when(strategy1.supports(any())).thenThrow(new RuntimeException("unexpected"));
        lenient().when(strategy1.getPriority()).thenReturn(0);

        EnhancedResponseCallback callback = new EnhancedResponseCallback(
                config, List.of(), List.of(strategy1), "/test"
        );

        HttpResponse result = callback.handle(HttpRequest.request().withPath("/test"));

        assertEquals(500, result.getStatusCode());
    }

    @Test
    void handleMergesGlobalHeaders() {
        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200).put("body", "ok");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        lenient().when(strategy1.supports(any())).thenReturn(true);
        lenient().when(strategy1.getPriority()).thenReturn(0);
        when(strategy1.handle(any(), any(), anyMap()))
                .thenReturn(HttpResponse.response().withStatusCode(200).withBody("ok"));

        EnhancedResponseCallback callback = new EnhancedResponseCallback(
                config,
                List.of(new GlobalHeader("X-Global", "value")),
                List.of(strategy1),
                "/test"
        );

        HttpResponse result = callback.handle(HttpRequest.request().withPath("/test"));

        assertEquals("value", result.getFirstHeader("X-Global"));
    }

    @Test
    void handleBuildsContextWithPathPattern() {
        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200);
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        lenient().when(strategy1.supports(any())).thenReturn(true);
        lenient().when(strategy1.getPriority()).thenReturn(0);
        when(strategy1.handle(any(), any(), anyMap()))
                .thenReturn(HttpResponse.response().withStatusCode(200));

        EnhancedResponseCallback callback = new EnhancedResponseCallback(
                config, null, List.of(strategy1), "/users/{id}"
        );

        callback.handle(HttpRequest.request().withPath("/users/42"));

        verify(strategy1).handle(any(), any(), argThat(ctx ->
                "/users/{id}".equals(ctx.get("pathPattern"))
        ));
    }
}
