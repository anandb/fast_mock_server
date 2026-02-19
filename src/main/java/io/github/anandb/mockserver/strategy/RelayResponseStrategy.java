package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.service.RelayService;
import io.github.anandb.mockserver.service.RelayService.RelayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy for relaying HTTP requests to a remote server.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelayResponseStrategy implements ResponseStrategy {

    private final RelayService relayService;

    @Override
    public HttpResponse handle(HttpRequest httpRequest, EnhancedExpectationDTO config, Map<String, Object> context) {
        try {
            // Extract request details
            String method = httpRequest.getMethod().getValue();
            String path = httpRequest.getPath().getValue();

            // Add query parameters to path if present
            if (httpRequest.getQueryStringParameters() != null && !httpRequest.getQueryStringParameters().isEmpty()) {
                String queryString = httpRequest.getQueryStringParameters().getEntries().stream()
                    .map(param -> param.getName().getValue() + "=" +
                         String.join(",", param.getValues().stream()
                             .map(val -> val.getValue())
                             .collect(Collectors.toList())))
                    .collect(Collectors.joining("&"));
                path = path + "?" + queryString;
            }

            // Convert headers to Map<String, List<String>>
            Map<String, List<String>> headers = null;
            if (httpRequest.getHeaders() != null && !httpRequest.getHeaders().isEmpty()) {
                headers = httpRequest.getHeaders().getEntries().stream()
                    .collect(Collectors.toMap(
                        header -> header.getName().getValue(),
                        header -> header.getValues().stream()
                            .map(val -> val.getValue())
                            .collect(Collectors.toList()),
                        (v1, v2) -> v1 // handle duplicates
                    ));
            }

            // Get request body
            byte[] body = null;
            if (httpRequest.getBody() != null) {
                String bodyString = httpRequest.getBodyAsString();
                if (bodyString != null && !bodyString.isEmpty()) {
                    body = bodyString.getBytes();
                }
            }

            // Relay the request
            RelayResponse relayResponse = relayService.relayRequest(
                config.getRelay(),
                method,
                path,
                headers,
                body
            );

            // Build MockServer HttpResponse from relay response
            HttpResponse response = HttpResponse.response()
                .withStatusCode(relayResponse.getStatusCode());

            // Add response headers
            if (relayResponse.getHeaders() != null) {
                for (Map.Entry<String, List<String>> header : relayResponse.getHeaders().entrySet()) {
                    if (header.getValue() != null && !header.getValue().isEmpty()) {
                        response = response.withHeader(header.getKey(),
                            header.getValue().toArray(String[]::new));
                    }
                }
            }

            // Add response body
            if (relayResponse.getBody() != null && relayResponse.getBody().length > 0) {
                response = response.withBody(relayResponse.getBody());
            }

            return response;

        } catch (Exception e) {
            log.error("Error relaying request", e);
            return HttpResponse.response()
                .withStatusCode(502)
                .withBody("Error relaying request to remote server: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(EnhancedExpectationDTO config) {
        return config.isRelay();
    }

    @Override
    public int getPriority() {
        return 30; // High priority, as relay usually overrides everything
    }
}
