package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Context object containing parsed HTTP request data for Freemarker template evaluation.
 * <p>
 * This model is used to pass HTTP request information (headers, body, cookies, path variables) as data
 * to Freemarker templates when processing dynamic response bodies.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequestContext {

    /**
     * Map of HTTP headers from the request.
     * Key: header name (String)
     * Value: header value (String, assumes only one value per header)
     */
    private Map<String, String> headers;

    /**
     * Parsed JSON body from the request.
     * A JsonNode which represents the JSON structure.
     */
    private JsonNode body;

    /**
     * Map of cookies from the request.
     * Key: cookie name (String)
     * Value: cookie value (String)
     */
    private Map<String, String> cookies;

    /**
     * Map of path variables from the request path.
     * Key: path variable name (String)
     * Value: path variable value (String)
     */
    private Map<String, String> pathVariables;
}
