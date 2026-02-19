package io.github.anandb.mockserver.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for common HTTP request processing tasks.
 */
@Slf4j
@UtilityClass
public class RequestUtils {

    /**
     * Extracts path variables from a request path based on a path pattern.
     *
     * @param requestPath the actual request path
     * @param pathPattern the expectation path pattern with variables in {brackets}
     * @return map of path variable names to values
     */
    public static Map<String, String> extractPathVariables(String requestPath, String pathPattern) {
        Map<String, String> pathVariables = new HashMap<>();

        if (pathPattern == null || requestPath == null) {
            return pathVariables;
        }

        String[] patternSegments = pathPattern.split("/");
        String[] pathSegments = requestPath.split("/");

        if (patternSegments.length != pathSegments.length) {
            log.debug("Path segment count mismatch: pattern has {} segments, request has {}",
                     patternSegments.length, pathSegments.length);
            return pathVariables;
        }

        for (int i = 0; i < patternSegments.length; i++) {
            String patternSegment = patternSegments[i];
            String pathSegment = pathSegments[i];

            if (patternSegment.startsWith("{") && patternSegment.endsWith("}")) {
                String variableName = patternSegment.substring(1, patternSegment.length() - 1);
                pathVariables.put(variableName, pathSegment);
            }
        }

        return pathVariables;
    }

    /**
     * Converts MockServer headers to a Map<String, List<String>>.
     *
     * @param request the MockServer HttpRequest
     * @return map of header names to list of values
     */
    public static Map<String, List<String>> headersToMap(HttpRequest request) {
        if (request.getHeaders() == null || request.getHeaders().isEmpty()) {
            return new HashMap<>();
        }

        return request.getHeaders().getEntries().stream()
                .collect(Collectors.toMap(
                        header -> header.getName().getValue(),
                        header -> header.getValues().stream()
                                .map(val -> val.getValue())
                                .collect(Collectors.toList()),
                        (v1, v2) -> v1 // Handle duplicates
                ));
    }
}
