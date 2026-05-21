package io.github.anandb.mockserver.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.model.TunnelConfig;
import io.github.anandb.mockserver.service.RelayService.RelayResponse;
import io.github.anandb.mockserver.util.HttpClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RelayService, verifying path matching logic
 * and HTTP request relaying behavior.
 */
@ExtendWith(MockitoExtension.class)
class RelayServiceTest {

    @Mock
    private OAuth2TokenService tokenService;

    @Mock
    private HttpClientFactory httpClientFactory;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<InputStream> httpResponse;

    @Captor
    private ArgumentCaptor<HttpRequest> requestCaptor;

    private RelayService relayService;

    @BeforeEach
    void setUp() {
        relayService = new RelayService(tokenService, httpClientFactory);
    }

    // --- findMatchingRelay tests ---

    @Test
    @DisplayName("Should find matching relay by path prefix")
    void testFindMatchingRelayFound() {
        RelayConfig relay = new RelayConfig();
        relay.setRemoteUrl("http://example.com");
        relay.setPrefixes(List.of("/api/**", "/other/**"));

        Optional<RelayConfig> result = relayService.findMatchingRelay(List.of(relay), "/api/v1/users");
        assertTrue(result.isPresent());
        assertSame(relay, result.get());
    }

    @Test
    @DisplayName("Should return empty when no prefix matches")
    void testFindMatchingRelayNotFound() {
        RelayConfig relay = new RelayConfig();
        relay.setRemoteUrl("http://example.com");
        relay.setPrefixes(List.of("/api/**"));

        Optional<RelayConfig> result = relayService.findMatchingRelay(List.of(relay), "/other/path");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return longest prefix match when multiple relays match")
    void testFindMatchingRelayLongestPrefix() {
        RelayConfig relay1 = new RelayConfig();
        relay1.setRemoteUrl("http://broad.com");
        relay1.setPrefixes(List.of("/api/**"));

        RelayConfig relay2 = new RelayConfig();
        relay2.setRemoteUrl("http://specific.com");
        relay2.setPrefixes(List.of("/api/v1/**"));

        Optional<RelayConfig> result = relayService.findMatchingRelay(
                List.of(relay1, relay2), "/api/v1/users");
        assertTrue(result.isPresent());
        assertSame(relay2, result.get());
    }

    @Test
    @DisplayName("Should return empty for null relay list")
    void testFindMatchingRelayNullList() {
        Optional<RelayConfig> result = relayService.findMatchingRelay(null, "/api/test");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty for empty relay list")
    void testFindMatchingRelayEmptyList() {
        Optional<RelayConfig> result = relayService.findMatchingRelay(List.of(), "/api/test");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should match all paths when prefixes is null (default /**)")
    void testFindMatchingRelayDefaultPrefix() {
        RelayConfig relay = new RelayConfig();
        relay.setRemoteUrl("http://example.com");
        relay.setPrefixes(null);

        Optional<RelayConfig> result = relayService.findMatchingRelay(List.of(relay), "/any/path");
        assertTrue(result.isPresent());
        assertSame(relay, result.get());
    }

    // --- relayRequest tests ---

    @Test
    @DisplayName("Should successfully relay a GET request")
    void testRelayGetRequest() throws Exception {
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("content-type", List.of("application/json")), (a, b) -> true));
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream("{\"ok\":true}".getBytes()));

        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("http://example.com");

        RelayResponse result = relayService.relayRequest(
                config, "GET", "/api/test", Map.of(), new byte[0]);

        assertEquals(200, result.statusCode());
        assertArrayEquals("{\"ok\":true}".getBytes(), result.body());
        assertTrue(result.headers().containsKey("content-type"));
    }

    @Test
    @DisplayName("Should add OAuth2 Bearer token when configured")
    void testRelayWithOAuth2Token() throws Exception {
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));
        when(httpResponse.body()).thenReturn(InputStream.nullInputStream());
        when(tokenService.getAccessToken(any(RelayConfig.class))).thenReturn("test-token");

        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("http://example.com");
        config.setTokenUrl("http://auth.example.com/token");
        config.setClientId("client");
        config.setClientSecret("secret");

        relayService.relayRequest(config, "GET", "/api/test", Map.of(), new byte[0]);

        HttpRequest captured = requestCaptor.getValue();
        String authHeader = captured.headers().firstValue("Authorization").orElse("");
        assertEquals("Bearer test-token", authHeader);
    }

    @Test
    @DisplayName("Should remove restricted headers from relayed request")
    void testRelayRemovesRestrictedHeaders() throws Exception {
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));
        when(httpResponse.body()).thenReturn(InputStream.nullInputStream());

        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("http://example.com");

        Map<String, List<String>> headers = Map.of(
                "Host", List.of("original-host.com"),
                "X-Custom", List.of("should-pass"),
                "Connection", List.of("keep-alive"));

        relayService.relayRequest(config, "GET", "/api/test", headers, new byte[0]);

        HttpRequest captured = requestCaptor.getValue();
        // Restricted headers should not be present
        assertTrue(captured.headers().firstValue("Host").isEmpty());
        assertTrue(captured.headers().firstValue("Connection").isEmpty());
        // Custom header should pass through
        assertEquals("should-pass", captured.headers().firstValue("X-Custom").orElse(""));
    }

    @Test
    @DisplayName("Should use tunnel URL when tunnel is enabled")
    void testRelayWithTunnel() throws Exception {
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));
        when(httpResponse.body()).thenReturn(InputStream.nullInputStream());

        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("http://remote.example.com");
        config.setAssignedHostPort(9999);
        TunnelConfig tunnelConfig = new TunnelConfig("test-ns", "test-pod", 8080);
        config.setTunnelConfig(tunnelConfig);

        relayService.relayRequest(config, "GET", "/api/test", Map.of(), new byte[0]);

        HttpRequest captured = requestCaptor.getValue();
        assertTrue(captured.uri().toString().startsWith("http://localhost:9999"));
    }

    @Test
    @DisplayName("Should handle POST method with body")
    void testRelayPostWithBody() throws Exception {
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));
        when(httpResponse.body()).thenReturn(InputStream.nullInputStream());

        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("http://example.com");

        byte[] body = "{\"name\":\"test\"}".getBytes();
        relayService.relayRequest(config, "POST", "/api/data", Map.of(), body);

        HttpRequest captured = requestCaptor.getValue();
        assertEquals("POST", captured.method());
    }
}
