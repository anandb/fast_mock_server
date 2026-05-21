package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectation;
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
import java.util.stream.Collectors;

import io.github.anandb.mockserver.util.ErrorCode;

/**
 * Strategy for relaying HTTP requests to a remote server.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelayResponseStrategy implements ResponseStrategy {

    private final RelayService relayService;

    @Override
    public HttpResponse handle(HttpRequest httpRequest, EnhancedExpectation config, Map<String, Object> context) {
        try {
            // Extract request details using centralized utility
            String method = httpRequest.getMethod().getValue();
            String path = httpRequest.getPath().getValue();
            String relayPath = path;

            // Add query parameters to relay path if present
            if (httpRequest.getQueryStringParameters() != null && !httpRequest.getQueryStringParameters().isEmpty()) {
                String queryString = httpRequest.getQueryStringParameters().getEntries().stream()
                    .map(param -> param.getName().getValue() + "=" +
                         String.join(",", param.getValues().stream()
                              .map(val -> val.getValue())
                              .toList()))
                    .collect(java.util.stream.Collectors.joining("&"));
                relayPath = path + "?" + queryString;
            }

            // Early log: helps when later request parsing fails or logs get delayed.
            log.info(
                "[RelayResponseStrategy] Incoming request (early) -> method={} path={} relayPath={}",
                method,
                path,
                relayPath
            );

            String requestHeaders = "";
            try {
                requestHeaders = RequestUtils.headersToMap(httpRequest).entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                    .collect(Collectors.joining("; "));
            } catch (Exception headersEx) {
                log.warn("[RelayResponseStrategy] Failed to serialize request headers for logging: {}", headersEx.getMessage());
            }

            Map<String, List<String>> headers;
            try {
                headers = RequestUtils.headersToMap(httpRequest);
            } catch (Exception headersEx) {
                log.warn("[RelayResponseStrategy] Failed to extract request headers for relay: {}", headersEx.getMessage());
                headers = java.util.Collections.emptyMap();
            }

            byte[] body = null;
            String bodyString = null;
            if (httpRequest.getBody() != null) {
                try {
                    bodyString = httpRequest.getBodyAsString();
                } catch (Exception bodyEx) {
                    log.warn("[RelayResponseStrategy] Failed to read request body for logging: {}", bodyEx.getMessage());
                }
                if (bodyString != null && !bodyString.isEmpty()) {
                    body = bodyString.getBytes();
                }
            }

            log.info(
                "[RelayResponseStrategy] Incoming request -> method={} path={} relayPath={} headers=[{}] body=[{}]",
                method,
                path,
                relayPath,
                requestHeaders,
                bodyString
            );

            // Get relays from context (server-level configuration)
            @SuppressWarnings("unchecked")
            List<RelayConfig> relays = (List<RelayConfig>) context.get("relays");

            Optional<RelayConfig> matchingRelay = relayService.findMatchingRelay(relays, path);

            if (matchingRelay.isEmpty()) {
                log.warn("No matching relay found for path: {}", path);
                return HttpResponse.response()
                    .withStatusCode(404)
                    .withBody(new ErrorCode("NO_MATCHING_RELAY", "No matching relay configuration found for: " + path).toString());
            }

            RelayResponse relayResponse = relayService.relayRequest(
                matchingRelay.get(),
                method,
                relayPath,
                headers,
                body
            );

            String relayResponseHeaders = "";
            if (relayResponse.headers() != null && !relayResponse.headers().isEmpty()) {
                relayResponseHeaders = relayResponse.headers().entrySet().stream()
                    .filter(entry -> entry.getKey() != null && !entry.getKey().startsWith(":"))
                    .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                    .collect(Collectors.joining("; "));
            }

            String relayResponseBody = null;
            if (relayResponse.body() != null && relayResponse.body().length > 0) {
                relayResponseBody = new String(relayResponse.body());
            }

            log.info(
                "[RelayResponseStrategy] Relayed response <- statusCode={} headers=[{}] body=[{}] (relayPath={})",
                relayResponse.statusCode(),
                relayResponseHeaders,
                relayResponseBody,
                relayPath
            );

            HttpResponse response = HttpResponse.response()
                .withStatusCode(relayResponse.statusCode());

            if (relayResponse.headers() != null) {
                for (Map.Entry<String, List<String>> header : relayResponse.headers().entrySet()) {
                    if (header.getKey().startsWith(":")) {
                        continue;
                    }
                    if (header.getValue() != null && !header.getValue().isEmpty()) {
                        response = response.withHeader(header.getKey(),
                            header.getValue().toArray(String[]::new));
                    }
                }
            }

            if (relayResponse.body() != null && relayResponse.body().length > 0) {
                response = response.withBody(relayResponse.body());
            }

            if (response != null) {
                log.info(
                    "[RelayResponseStrategy] Final response -> statusCode={} (contentLength={})",
                    relayResponse.statusCode(),
                    relayResponse.body() == null ? 0 : relayResponse.body().length
                );
            }

            return response;

        } catch (Exception e) {
            log.error("Error relaying request", e);
            return HttpResponse.response()
                .withStatusCode(502)
                .withBody(new ErrorCode("RELAY_ERROR", "Error relaying request to remote server: " + e.getMessage()).toString());
        }
    }

    @Override
    public boolean supports(EnhancedExpectation config) {
        // Support if it's explicitly NOT an SSE or File response and has no static body defined
        // This is a heuristic for "relay DTO" created by MockServerManager
        return !config.isSse() && !config.isFileResponse() && config.getHttpResponse() == null;
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
