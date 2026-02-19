package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.Map;

/**
 * Strategy interface for generating HTTP responses based on enhanced expectation configurations.
 */
public interface ResponseStrategy {

    /**
     * Handles the request and returns an HTTP response.
     *
     * @param request the incoming HTTP request
     * @param config the enhanced expectation configuration
     * @param context shared context (path variables, cookies, etc.)
     * @return the generated HTTP response
     */
    HttpResponse handle(HttpRequest request, EnhancedExpectationDTO config, Map<String, Object> context);

    /**
     * Determines if this strategy supports the given configuration.
     *
     * @param config the enhanced expectation configuration
     * @return true if supported, false otherwise
     */
    boolean supports(EnhancedExpectationDTO config);

    /**
     * Returns the priority of this strategy. Higher values are checked first.
     *
     * @return the priority value
     */
    default int getPriority() {
        return 0;
    }
}
