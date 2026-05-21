package io.github.anandb.mockserver.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.anandb.mockserver.model.EnhancedExpectation;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.service.RelayService;
import io.github.anandb.mockserver.service.RelayService.RelayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RelayResponseStrategy Tests")
@ExtendWith(MockitoExtension.class)
class RelayResponseStrategyTest {

    @Mock
    private RelayService relayService;

    private RelayResponseStrategy strategy;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        strategy = new RelayResponseStrategy(relayService);
        mapper = new ObjectMapper();
    }

    @Test
    void supportsReturnsTrueWhenNotSseNotFileNoHttpResponse() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertTrue(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseWhenSse() {
        ObjectNode httpResponse = mapper.createObjectNode();
        httpResponse.set("messages", mapper.createArrayNode().add("msg"));
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .sse(true)
                .build();
        assertFalse(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseWhenFileResponse() {
        ObjectNode httpResponse = mapper.createObjectNode().put("file", "/path");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();
        assertFalse(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseWhenHttpResponsePresent() {
        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200);
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();
        assertFalse(strategy.supports(config));
    }

    @Test
    void handleRelaysRequestSuccessfully() throws Exception {
        EnhancedExpectation config = new EnhancedExpectation();
        RelayConfig relayConfig = new RelayConfig();
        relayConfig.setRemoteUrl("https://api.example.com");
        List<RelayConfig> relays = List.of(relayConfig);

        when(relayService.findMatchingRelay(anyList(), anyString()))
                .thenReturn(Optional.of(relayConfig));
        when(relayService.relayRequest(any(), anyString(), anyString(), anyMap(), any()))
                .thenReturn(new RelayResponse(200, Map.of("Content-Type", List.of("application/json")), "{\"ok\":true}".getBytes()));

        HttpRequest request = HttpRequest.request()
                .withMethod("GET")
                .withPath("/api/data");

        HttpResponse result = strategy.handle(request, config, Map.of("relays", relays));

        assertEquals(200, result.getStatusCode());
        assertEquals("application/json", result.getFirstHeader("Content-Type"));
    }

    @Test
    void handleReturns404WhenNoMatchingRelay() throws Exception {
        EnhancedExpectation config = new EnhancedExpectation();

        when(relayService.findMatchingRelay(anyList(), anyString()))
                .thenReturn(Optional.empty());

        HttpRequest request = HttpRequest.request()
                .withMethod("GET")
                .withPath("/api/data");

        HttpResponse result = strategy.handle(request, config, Map.of("relays", List.of()));

        assertEquals(404, result.getStatusCode());
    }

    @Test
    void handleReturns502OnRelayError() throws Exception {
        EnhancedExpectation config = new EnhancedExpectation();
        RelayConfig relayConfig = new RelayConfig();
        relayConfig.setRemoteUrl("https://api.example.com");

        when(relayService.findMatchingRelay(anyList(), anyString()))
                .thenReturn(Optional.of(relayConfig));
        when(relayService.relayRequest(any(), anyString(), anyString(), anyMap(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        HttpRequest request = HttpRequest.request()
                .withMethod("GET")
                .withPath("/api/data");

        HttpResponse result = strategy.handle(request, config, Map.of("relays", List.of(relayConfig)));

        assertEquals(502, result.getStatusCode());
    }

    @Test
    void handleIncludesQueryParameters() throws Exception {
        EnhancedExpectation config = new EnhancedExpectation();
        RelayConfig relayConfig = new RelayConfig();
        relayConfig.setRemoteUrl("https://api.example.com");

        when(relayService.findMatchingRelay(anyList(), anyString()))
                .thenReturn(Optional.of(relayConfig));
        when(relayService.relayRequest(any(), anyString(), contains("?"), anyMap(), any()))
                .thenReturn(new RelayResponse(200, Map.of(), new byte[0]));

        HttpRequest request = HttpRequest.request()
                .withMethod("GET")
                .withPath("/api/data")
                .withQueryStringParameter(NottableString.string("key"), NottableString.string("value"));

        strategy.handle(request, config, Map.of("relays", List.of(relayConfig)));

        verify(relayService).relayRequest(any(), anyString(), argThat(path -> path.contains("?")), anyMap(), any());
    }

    @Test
    void getPriorityIs30() {
        assertEquals(30, strategy.getPriority());
    }
}
