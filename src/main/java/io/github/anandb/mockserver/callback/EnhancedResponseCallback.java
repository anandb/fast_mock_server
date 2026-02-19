package io.github.anandb.mockserver.callback;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import io.github.anandb.mockserver.util.RequestUtils;
import io.github.anandb.mockserver.util.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Universal callback that delegates to specialized response strategies.
 * Handles header merging, context building, and error handling.
 */
@Slf4j
public class EnhancedResponseCallback implements ExpectationResponseCallback {

    private final EnhancedExpectationDTO config;
    private final List<GlobalHeader> globalHeaders;
    private final List<ResponseStrategy> strategies;
    private final String pathPattern;
    private final List<RelayConfig> relays;

    public EnhancedResponseCallback(
            EnhancedExpectationDTO config,
            List<GlobalHeader> globalHeaders,
            List<ResponseStrategy> strategies,
            String pathPattern) {
        this(config, globalHeaders, strategies, pathPattern, null);
    }

    public EnhancedResponseCallback(
            EnhancedExpectationDTO config,
            List<GlobalHeader> globalHeaders,
            List<ResponseStrategy> strategies,
            String pathPattern,
            List<RelayConfig> relays) {
        this.config = config;
        this.globalHeaders = globalHeaders;
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(ResponseStrategy::getPriority).reversed())
                .collect(Collectors.toList());
        this.pathPattern = pathPattern;
        this.relays = relays;
    }

    @Override
    public HttpResponse handle(HttpRequest httpRequest) {
        try {
            log.debug("Handling request for {} {}", httpRequest.getMethod(), httpRequest.getPath());

            Map<String, Object> context = new HashMap<>();
            context.put("pathPattern", pathPattern);

            // Extract path variables for all strategies
            Map<String, String> pathVars = RequestUtils.extractPathVariables(
                    httpRequest.getPath().getValue(),
                    pathPattern);
            context.put("pathVariables", pathVars);

            // Pass relays to context if available
            if (relays != null) {
                context.put("relays", relays);
            }

            ResponseStrategy strategy = strategies.stream()
                    .filter(s -> s.supports(config))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No strategy found for configuration"));

            HttpResponse response = strategy.handle(httpRequest, config, context);

            return ResponseUtils.mergeGlobalHeaders(response, globalHeaders);

        } catch (Exception e) {
            log.error("Error in enhanced callback", e);
            return HttpResponse.response()
                    .withStatusCode(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                            {
                              "errorCode": "CALLBACK_ERROR",
                              "message": "%s"
                            }
                            """.formatted(e.getMessage()));
        }
    }
}
