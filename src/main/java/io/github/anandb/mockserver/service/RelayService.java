package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.RelayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for relaying HTTP requests to remote servers with optional OAuth2 authentication.
 * <p>
 * This service handles forwarding incoming requests to remote endpoints, including
 * optional OAuth2 token acquisition and custom header management.
 * </p>
 */
@Service
@Slf4j
public class RelayService {

    private final OAuth2TokenService tokenService;
    private final HttpClient httpClient;

    public RelayService(OAuth2TokenService tokenService) {
        this.tokenService = tokenService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Relays an HTTP request to the remote server configured in the relay config.
     *
     * @param relayConfig the relay configuration
     * @param method the HTTP method
     * @param path the request path
     * @param headers the request headers
     * @param body the request body (can be null)
     * @return the relay response containing status code, headers, and body
     * @throws Exception if the relay fails
     */
    public RelayResponse relayRequest(RelayConfig relayConfig, String method, String path,
                                       Map<String, List<String>> headers, byte[] body) throws Exception {
        // Build target URL
        String targetUrl = buildTargetUrl(relayConfig.getRemoteUrl(), path);
        log.info("Relaying {} request to: {}", method, targetUrl);

        // Build HTTP request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(60));

        // Add Authorization header with Bearer token if OAuth2 is enabled
        if (relayConfig.isOAuth2Enabled()) {
            String accessToken = tokenService.getAccessToken(relayConfig);
            requestBuilder.header("Authorization", "Bearer " + accessToken);
            log.debug("Added OAuth2 Bearer token to request");
        } else {
            log.debug("OAuth2 not configured, relaying without authentication");
        }

        // Add custom headers from relay config
        if (relayConfig.hasHeaders()) {
            for (Map.Entry<String, String> header : relayConfig.getHeaders().entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }
        }

        // Add original request headers (excluding host, connection, etc.)
        if (headers != null) {
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                String headerName = header.getKey();
                // Skip certain headers that shouldn't be forwarded
                if (shouldForwardHeader(headerName)) {
                    for (String value : header.getValue()) {
                        requestBuilder.header(headerName, value);
                    }
                }
            }
        }

        // Set HTTP method and body
        HttpRequest.BodyPublisher bodyPublisher = body != null && body.length > 0
                ? HttpRequest.BodyPublishers.ofByteArray(body)
                : HttpRequest.BodyPublishers.noBody();

        switch (method.toUpperCase()) {
            case "GET" -> requestBuilder.GET();
            case "POST" -> requestBuilder.POST(bodyPublisher);
            case "PUT" -> requestBuilder.PUT(bodyPublisher);
            case "DELETE" -> requestBuilder.DELETE();
            case "PATCH" -> requestBuilder.method("PATCH", bodyPublisher);
            case "HEAD" -> requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            case "OPTIONS" -> requestBuilder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
            default -> requestBuilder.method(method, bodyPublisher);
        }

        HttpRequest request = requestBuilder.build();

        // Send request
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Read response body
        byte[] responseBody = readAllBytes(response.body());

        log.info("Relay response status: {}", response.statusCode());

        return new RelayResponse(response.statusCode(), response.headers().map(), responseBody);
    }

    /**
     * Builds the target URL by combining the remote URL with the request path.
     *
     * @param remoteUrl the base remote URL
     * @param path the request path
     * @return the complete target URL
     */
    private String buildTargetUrl(String remoteUrl, String path) {
        String baseUrl = remoteUrl.endsWith("/") ? remoteUrl.substring(0, remoteUrl.length() - 1) : remoteUrl;
        String requestPath = path.startsWith("/") ? path : "/" + path;
        return baseUrl + requestPath;
    }

    /**
     * Determines if a header should be forwarded to the remote server.
     *
     * @param headerName the header name
     * @return true if the header should be forwarded, false otherwise
     */
    private boolean shouldForwardHeader(String headerName) {
        String lowerCaseName = headerName.toLowerCase();
        // Skip headers that shouldn't be forwarded
        return !lowerCaseName.equals("host") &&
               !lowerCaseName.equals("connection") &&
               !lowerCaseName.equals("content-length") &&
               !lowerCaseName.equals("transfer-encoding") &&
               !lowerCaseName.equals("authorization"); // We set our own Authorization header
    }

    /**
     * Reads all bytes from an InputStream.
     *
     * @param inputStream the input stream
     * @return the byte array
     * @throws Exception if reading fails
     */
    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Represents a relay response from the remote server.
     */
    public static class RelayResponse {
        private final int statusCode;
        private final Map<String, List<String>> headers;
        private final byte[] body;

        public RelayResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public byte[] getBody() {
            return body;
        }
    }
}
