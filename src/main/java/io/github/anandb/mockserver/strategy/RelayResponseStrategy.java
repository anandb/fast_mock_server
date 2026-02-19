package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.service.RelayService;
import io.github.anandb.mockserver.service.RelayService.RelayResponse;
import io.github.anandb.mockserver.util.RequestUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            // Extract request details using centralized utility
            String method = httpRequest.getMethod().getValue();
            String path = httpRequest.getPath().getValue();

            // Add query parameters to path if present
            if (httpRequest.getQueryStringParameters() != null && !httpRequest.getQueryStringParameters().isEmpty()) {
                String queryString = httpRequest.getQueryStringParameters().getEntries().stream()
                    .map(param -> param.getName().getValue() + "=" +
                         String.join(",", param.getValues().stream()
                             .map(val -> val.getValue())
                             .toList()))
                    .collect(java.util.stream.Collectors.joining("&"));
                path = path + "?" + queryString;
            }

            Map<String, List<String>> headers = RequestUtils.headersToMap(httpRequest);

            byte[] body = null;
            if (httpRequest.getBody() != null) {
                String bodyString = httpRequest.getBodyAsString();
                if (bodyString != null && !bodyString.isEmpty()) {
                    body = bodyString.getBytes();
                }
            }

            // Get relays from context (server-level configuration)
            @SuppressWarnings("unchecked")
            List<RelayConfig> relays = (List<RelayConfig>) context.get("relays");

            Optional<RelayConfig> matchingRelay = relayService.findMatchingRelay(relays, path);

            if (matchingRelay.isEmpty()) {
                log.warn("No matching relay found for path: {}", path);
                return HttpResponse.response()
                    .withStatusCode(404)
                    .withBody(
                        """
                        {
                            "errorCode": "NO_MATCHING_RELAY",
                            "message": "No matching relay configuration found for path: %s"
                        }
                        """.formatted(path));
            }

            RelayResponse relayResponse = relayService.relayRequest(
                matchingRelay.get(),
                method,
                path,
                headers,
                body
            );

            HttpResponse response = HttpResponse.response()
                .withStatusCode(relayResponse.statusCode());

            if (relayResponse.headers() != null) {
                for (Map.Entry<String, List<String>> header : relayResponse.headers().entrySet()) {
                    if (header.getValue() != null && !header.getValue().isEmpty()) {
                        response = response.withHeader(header.getKey(),
                            header.getValue().toArray(String[]::new));
                    }
                }
            }

            if (relayResponse.body() != null && relayResponse.body().length > 0) {
                response = response.withBody(relayResponse.body());
            }

            return response;

        } catch (Exception e) {
            log.error("Error relaying request", e);
            return HttpResponse.response()
                .withStatusCode(502)
                .withBody(
                    """
                    {
                        "errorCode": "RELAY_ERROR",
                        "message": "Error relaying request to remote server: %s"
                    }
                    """.formatted(e.getMessage()));
        }
    }

    @Override
    public boolean supports(EnhancedExpectationDTO config) {
        return false;
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
