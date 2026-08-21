package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Strategy for handling Server-Sent Events (SSE) responses.
 */
@Component
public class SSEResponseStrategy implements ResponseStrategy {
    private static final Logger log = LoggerFactory.getLogger(SSEResponseStrategy.class);

    @Override
    public HttpResponse handle(HttpRequest request, EnhancedExpectation config, Map<String, Object> context) {
        List<String> messages = config.getMessages();
        log.info("Handling SSE request for {} {} with {} messages", request.getMethod(), request.getPath(), messages.size());
        StringBuilder sseBody = new StringBuilder();
        for (String message : messages) {
            sseBody.append("data: ").append(message).append("\n\n");
        }
        int responseCode = 200;
        List<Header> responseHeaders = null;
        try {
            org.mockserver.model.HttpResponse httpResponse = config.getHttpResponse();
            if (httpResponse != null) {
                responseCode = httpResponse.getStatusCode();
                responseHeaders = new java.util.ArrayList<>(httpResponse.getHeaderList());
            }
        } catch (Exception e) {
            log.warn("Could not parse HTTP response config, using defaults: {}", e.getMessage());
        }
        org.mockserver.model.HttpResponse response = HttpResponse.response().withStatusCode(responseCode).withHeader("Content-Type", "text/event-stream").withHeader("Cache-Control", "no-cache").withHeader("Connection", "keep-alive").withBody(sseBody.toString());
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            response.withHeaders(responseHeaders);
        }
        return response;
    }

    @Override
    public boolean supports(EnhancedExpectation config) {
        return config.isSse();
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
