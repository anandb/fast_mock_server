package io.github.anandb.mockserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.anandb.mockserver.exception.OAuth2Exception;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.util.HttpClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2TokenService Tests")
class OAuth2TokenServiceTest {

    @Mock
    private HttpClientFactory httpClientFactory;

    @Mock
    private HttpClient httpClient;

    private OAuth2TokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tokenService = new OAuth2TokenService(objectMapper, httpClientFactory);
    }

    @Test
    void getAccessTokenReturnsTokenFromSuccessfulResponse() throws Exception {
        RelayConfig config = createRelayConfig();

        String responseBody = "{\"access_token\":\"test-token-abc\",\"expires_in\":3600}";
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        String token = tokenService.getAccessToken(config);

        assertEquals("test-token-abc", token);
    }

    @Test
    void getAccessTokenCachesTokenOnSubsequentCalls() throws Exception {
        RelayConfig config = createRelayConfig();

        String responseBody = "{\"access_token\":\"cached-token\",\"expires_in\":3600}";
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        // First call — fetches token
        String token1 = tokenService.getAccessToken(config);
        // Second call — should use cache (no second HTTP call)
        String token2 = tokenService.getAccessToken(config);

        assertEquals("cached-token", token1);
        assertEquals("cached-token", token2);
        // HTTP client should only be called once (for the first request)
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void getAccessTokenThrowsOnNon200Response() throws Exception {
        RelayConfig config = createRelayConfig();

        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("{\"error\":\"invalid_client\"}");
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        OAuth2Exception ex = assertThrows(OAuth2Exception.class,
            () -> tokenService.getAccessToken(config));
        assertTrue(ex.getMessage().contains("401"));
    }

    @Test
    void getAccessTokenThrowsWhenAccessTokenMissingInResponse() throws Exception {
        RelayConfig config = createRelayConfig();

        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"token_type\":\"bearer\"}");
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        OAuth2Exception ex = assertThrows(OAuth2Exception.class,
            () -> tokenService.getAccessToken(config));
        assertTrue(ex.getMessage().contains("Access token not found"));
    }

    @Test
    void getAccessTokenIncludesScopeWhenProvided() throws Exception {
        RelayConfig config = createRelayConfig();
        config.setScope("read write");

        String responseBody = "{\"access_token\":\"scoped-token\",\"expires_in\":3600}";
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        String token = tokenService.getAccessToken(config);
        assertEquals("scoped-token", token);

        // Verify the request was built (scope included in form body)
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void getAccessTokenSkipsScopeWhenBlank() throws Exception {
        RelayConfig config = createRelayConfig();
        config.setScope("  ");

        String responseBody = "{\"access_token\":\"no-scope\",\"expires_in\":3600}";
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        String token = tokenService.getAccessToken(config);
        assertEquals("no-scope", token);
    }

    @Test
    void getAccessTokenUsesDefaultExpiryWhenMissing() throws Exception {
        RelayConfig config = createRelayConfig();

        // No expires_in field
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"access_token\":\"token-no-expiry\"}");
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        String token = tokenService.getAccessToken(config);
        assertEquals("token-no-expiry", token);
    }

    @Test
    void clearCacheRemovesCachedTokens() throws Exception {
        RelayConfig config = createRelayConfig();

        String responseBody = "{\"access_token\":\"temp-token\",\"expires_in\":3600}";
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClientFactory.getHttpClient(anyBoolean())).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        tokenService.getAccessToken(config);
        tokenService.clearCache();

        // After clearing cache, next call should fetch again
        String responseBody2 = "{\"access_token\":\"fresh-token\",\"expires_in\":3600}";
        HttpResponse<String> httpResponse2 = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse2.statusCode()).thenReturn(200);
        when(httpResponse2.body()).thenReturn(responseBody2);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse2);

        String token = tokenService.getAccessToken(config);
        assertEquals("fresh-token", token);
        // Two HTTP calls: one before clear, one after
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    private RelayConfig createRelayConfig() {
        RelayConfig config = new RelayConfig();
        config.setTokenUrl("https://auth.example.com/token");
        config.setClientId("my-client");
        config.setClientSecret("my-secret");
        config.setGrantType("client_credentials");
        config.setIgnoreSSLErrors(false);
        return config;
    }
}
