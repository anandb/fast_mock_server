package io.github.anandb.mockserver.callback;

import io.github.anandb.mockserver.model.EnhancedExpectation;
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

import io.github.anandb.mockserver.util.ErrorCode;

/**
 * Universal callback that delegates to specialized response strategies.
 * Handles header merging, context building, and error handling.
 */
@Slf4j
public class EnhancedResponseCallback implements ExpectationResponseCallback {

    private final EnhancedExpectation config;
    private final List<GlobalHeader> globalHeaders;
    private final List<ResponseStrategy> strategies;
    private final String pathPattern;
    private final List<RelayConfig> relays;

    public EnhancedResponseCallback(
            EnhancedExpectation config,
            List<GlobalHeader> globalHeaders,
            List<ResponseStrategy> strategies,
            String pathPattern) {
        this(config, globalHeaders, strategies, pathPattern, null);
    }

    public EnhancedResponseCallback(
            EnhancedExpectation config,
            List<GlobalHeader> globalHeaders,
            List<ResponseStrategy> strategies,
            String pathPattern,
            List<RelayConfig> relays) {
        this.config = config;
        this.globalHeaders = globalHeaders != null ? globalHeaders : List.of();
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(ResponseStrategy::getPriority).reversed())
                .toList();
        this.pathPattern = pathPattern;
        this.relays = relays;
    }

    @Override
    public HttpResponse handle(HttpRequest httpRequest) {
        try {
            log.info("Callback handling {}", httpRequest);
            Map<String, Object> context = buildContext(httpRequest);
            ResponseStrategy strategy = findSupportingStrategy();

            return ResponseUtils.mergeGlobalHeaders(
                strategy.handle(httpRequest, config, context),
                globalHeaders
            );
        } catch (Exception e) {
            log.error("Error in enhanced callback: {}", e.getMessage());
            return HttpResponse.response()
                    .withStatusCode(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody(new ErrorCode("CALLBACK_ERROR", e.getMessage()).toString());
        }
    }

    private Map<String, Object> buildContext(HttpRequest httpRequest) {
        Map<String, Object> context = new HashMap<>();
        context.put("pathPattern", pathPattern);
        context.put("pathVariables", extractPathVariables(httpRequest));
        if (relays != null) {
            context.put("relays", relays);
        }
        return context;
    }

    private ResponseStrategy findSupportingStrategy() {
        return strategies.stream()
                .filter(s -> s.supports(config))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No strategy found for configuration"));
    }

    private Map<String, String> extractPathVariables(HttpRequest httpRequest) {
        if (httpRequest != null && httpRequest.getPath() != null && pathPattern != null) {
            return RequestUtils.extractPathVariables(httpRequest.getPath().getValue(), pathPattern);
        }
        return Map.of();
    }
}
