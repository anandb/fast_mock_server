package com.example.mockserver.model;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Context object containing parsed HTTP request data for Freemarker template evaluation.
 * <p>
 * This model is used to pass HTTP request information (headers, body, cookies) as data
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
     * A JsonObject which is essentially a map of String to Object.
     */
    private JsonObject body;

    /**
     * Map of cookies from the request.
     * Key: cookie name (String)
     * Value: cookie value (String)
     */
    private Map<String, String> cookies;
}
