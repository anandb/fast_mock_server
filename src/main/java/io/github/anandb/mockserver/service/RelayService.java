package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.util.MapperSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for relaying requests to remote servers with OAuth2 support.
 */
@Slf4j
@Service
public class RelayService {

    private final HttpClient httpClient;
    private final OAuth2TokenService tokenService;
    private final ObjectMapper objectMapper;

    public RelayService(OAuth2TokenService tokenService) {
        this.tokenService = tokenService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = MapperSupplier.getMapper();
    }

    /**
     * Relays an HTTP request to a remote server.
     */
    public RelayResponse relayRequest(
            RelayConfig config,
            String method,
            String path,
            Map<String, List<String>> headers,
            byte[] body) throws Exception {

        String remoteUrl = config.getRemoteUrl();
        if (remoteUrl.endsWith("/") && path.startsWith("/")) {
            remoteUrl = remoteUrl.substring(0, remoteUrl.length() - 1);
        }
        String targetUrl = remoteUrl + path;

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(30));

        // Apply original headers
        if (headers != null) {
            headers.forEach((name, values) -> {
                if (!isRestrictedHeader(name)) {
                    values.forEach(value -> requestBuilder.header(name, value));
                }
            });
        }

        // Apply custom relay headers
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(requestBuilder::header);
        }

        // Apply OAuth2 token if configured
        if (config.isValid() && config.getTokenUrl() != null) {
            String token = tokenService.getAccessToken(config);
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher bodyPublisher = (body != null && body.length > 0)
                ? HttpRequest.BodyPublishers.ofByteArray(body)
                : HttpRequest.BodyPublishers.noBody();

        // Modern switch expression for HTTP methods
        switch (method.toUpperCase()) {
            case "GET" -> requestBuilder.GET();
            case "POST" -> requestBuilder.POST(bodyPublisher);
            case "PUT" -> requestBuilder.PUT(bodyPublisher);
            case "DELETE" -> requestBuilder.DELETE();
            case "PATCH" -> requestBuilder.method("PATCH", bodyPublisher);
            case "HEAD" -> requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            case "OPTIONS" -> requestBuilder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
            default -> requestBuilder.method(method.toUpperCase(), bodyPublisher);
        }

        HttpResponse<InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

        Map<String, List<String>> responseHeaders = response.headers().map();
        byte[] responseBody;
        try (InputStream is = response.body()) {
            responseBody = is.readAllBytes();
        }

        return new RelayResponse(response.statusCode(), responseHeaders, responseBody);
    }

    private boolean isRestrictedHeader(String name) {
        return name.equalsIgnoreCase("Host") ||
               name.equalsIgnoreCase("Content-Length") ||
               name.equalsIgnoreCase("Connection") ||
               name.equalsIgnoreCase("Upgrade");
    }

    public record RelayResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {}
}
