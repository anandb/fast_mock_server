package io.github.anandb.mockserver.callback;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockserver.model.Header.header;

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

    public EnhancedResponseCallback(
            EnhancedExpectationDTO config,
            List<GlobalHeader> globalHeaders,
            List<ResponseStrategy> strategies,
            String pathPattern) {
        this.config = config;
        this.globalHeaders = globalHeaders;
        // Sort strategies by priority
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(ResponseStrategy::getPriority).reversed())
                .collect(Collectors.toList());
        this.pathPattern = pathPattern;
    }

    @Override
    public HttpResponse handle(HttpRequest httpRequest) {
        try {
            log.debug("Handling request for {} {}", httpRequest.getMethod(), httpRequest.getPath());

            // 1. Prepare Context
            Map<String, Object> context = new HashMap<>();
            context.put("pathPattern", pathPattern);

            // 2. Select Strategy
            ResponseStrategy strategy = strategies.stream()
                    .filter(s -> s.supports(config))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No strategy found for configuration"));

            // 3. Generate Response
            HttpResponse response = strategy.handle(httpRequest, config, context);

            // 4. Merge Global Headers
            return mergeGlobalHeaders(response);

        } catch (Exception e) {
            log.error("Error in enhanced callback", e);
            return HttpResponse.response()
                    .withStatusCode(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody(String.format("{\"errorCode\":\"CALLBACK_ERROR\",\"message\":\"%s\"}", e.getMessage()));
        }
    }

    private HttpResponse mergeGlobalHeaders(HttpResponse response) {
        if (globalHeaders == null || globalHeaders.isEmpty()) {
            return response;
        }

        // Get existing headers from response
        List<Header> existingHeaders = response.getHeaderList() != null
                ? new ArrayList<>(response.getHeaderList())
                : new ArrayList<>();

        // Convert existing headers to map for easy lookup
        Map<NottableString, Header> headerMap = existingHeaders.stream().collect(Collectors.toMap(
                Header::getName,
                h -> h,
                (h1, h2) -> h1
        ));

        // Add global headers (only if not already present)
        for (GlobalHeader globalHeader : globalHeaders) {
            NottableString headerName = NottableString.string(globalHeader.getName());
            if (!headerMap.containsKey(headerName)) {
                headerMap.put(headerName, header(globalHeader.getName(), globalHeader.getValue()));
            }
        }

        return response.withHeaders(new ArrayList<>(headerMap.values()));
    }
}
