package io.github.anandb.mockserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.util.HttpClientFactory;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing OAuth2 token acquisition and caching.
 * <p>
 * This service fetches access tokens from OAuth2 token endpoints using client credentials grant
 * and caches them to avoid unnecessary token requests.
 * </p>
 */
@Service
@Slf4j
public class OAuth2TokenService {

    private final HttpClientFactory httpClientFactory;
    private final ObjectMapper objectMapper;
    private final Map<String, TokenCache> tokenCacheMap;

    public OAuth2TokenService(ObjectMapper objectMapper, HttpClientFactory httpClientFactory) {
        this.objectMapper = objectMapper;
        this.httpClientFactory = httpClientFactory;
        this.tokenCacheMap = new ConcurrentHashMap<>();
    }

    /**
     * Gets an access token for the given relay configuration.
     * Returns a cached token if available and not expired, otherwise fetches a new one.
     *
     * @param relayConfig the relay configuration containing OAuth2 credentials
     * @return the access token
     * @throws Exception if token acquisition fails
     */
    public String getAccessToken(RelayConfig relayConfig) throws Exception {
        String cacheKey = generateCacheKey(relayConfig);
        TokenCache cache = tokenCacheMap.get(cacheKey);

        // Check if cached token is still valid
        if (cache != null && !cache.isExpired()) {
            log.debug("Using cached access token for {}", relayConfig.getTokenUrl());
            return cache.getAccessToken();
        }

        // Fetch new token
        log.info("Fetching new access token from {}", relayConfig.getTokenUrl());
        String accessToken = fetchAccessToken(relayConfig);

        // Cache the token (default expiry: 3600 seconds - 5 minutes for safety)
        tokenCacheMap.put(cacheKey, new TokenCache(accessToken, 3300));

        return accessToken;
    }

    /**
     * Fetches a new access token from the OAuth2 token endpoint.
     *
     * @param relayConfig the relay configuration containing OAuth2 credentials
     * @return the access token
     * @throws Exception if token acquisition fails
     */
    private String fetchAccessToken(RelayConfig relayConfig) throws Exception {
        // Build form data
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", relayConfig.getGrantType());
        formData.put("client_id", relayConfig.getClientId());
        formData.put("client_secret", relayConfig.getClientSecret());

        if (relayConfig.getScope() != null && !relayConfig.getScope().isBlank()) {
            formData.put("scope", relayConfig.getScope());
        }

        String formBody = buildFormBody(formData);

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(relayConfig.getTokenUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        // Send request
        HttpClient httpClient = httpClientFactory.getHttpClient(relayConfig.isIgnoreSSLErrors());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch access token. Status: " + response.statusCode() +
                                     ", Body: " + response.body());
        }

        // Parse response
        JsonNode jsonNode = objectMapper.readTree(response.body());
        if (!jsonNode.has("access_token")) {
            throw new RuntimeException("Access token not found in response: " + response.body());
        }

        return jsonNode.get("access_token").asText();
    }

    /**
     * Builds form-encoded body from key-value pairs.
     *
     * @param formData the form data
     * @return the form-encoded string
     */
    private String buildFormBody(Map<String, String> formData) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey())
              .append("=")
              .append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Generates a cache key for the relay configuration.
     *
     * @param relayConfig the relay configuration
     * @return the cache key
     */
    private String generateCacheKey(RelayConfig relayConfig) {
        return relayConfig.getTokenUrl() + ":" + relayConfig.getClientId();
    }

    /**
     * Clears all cached tokens.
     */
    public void clearCache() {
        tokenCacheMap.clear();
    }

    /**
     * Internal class for caching tokens with expiry.
     */
    private static class TokenCache {
        private final String accessToken;
        private final long expiryTime;

        public TokenCache(String accessToken, long expiresInSeconds) {
            this.accessToken = accessToken;
            this.expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000);
        }

        public String getAccessToken() {
            return accessToken;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }
    }
}
