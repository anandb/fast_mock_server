package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Strategy for handling standard static MockServer responses.
 */
@Component
public class StaticResponseStrategy implements ResponseStrategy {

    @Override
    public HttpResponse handle(HttpRequest request, EnhancedExpectationDTO config, Map<String, Object> context) {
        return config.getHttpResponse();
    }

    @Override
    public boolean supports(EnhancedExpectationDTO config) {
        // Support if no other enhancement is present
        return !config.isSse() && !config.isFileResponse() && !config.isRelay();
    }

    @Override
    public int getPriority() {
        return -100; // Lowest priority
    }
}
