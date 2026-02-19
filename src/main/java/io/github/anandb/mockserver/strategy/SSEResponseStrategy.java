package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Strategy for handling Server-Sent Events (SSE) responses.
 */
@Slf4j
@Component
public class SSEResponseStrategy implements ResponseStrategy {

    @Override
    public HttpResponse handle(HttpRequest request, EnhancedExpectationDTO config, Map<String, Object> context) {
        List<String> messages = config.getMessages();
        log.info("Handling SSE request for {} {} with {} messages", 
                request.getMethod(), request.getPath(), messages.size());

        StringBuilder sseBody = new StringBuilder();
        for (String message : messages) {
            sseBody.append("data: ").append(message).append("\n\n");
        }

        Integer responseCode = config.getHttpResponse() != null ? config.getHttpResponse().getStatusCode() : 200;

        return HttpResponse.response()
                .withStatusCode(responseCode)
                .withHeaders(config.getHttpResponse() != null ? config.getHttpResponse().getHeaderList() : null)
                .withHeader("Content-Type", "text/event-stream")
                .withHeader("Cache-Control", "no-cache")
                .withHeader("Connection", "keep-alive")
                .withBody(sseBody.toString());
    }

    @Override
    public boolean supports(EnhancedExpectationDTO config) {
        return config.isSse();
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
