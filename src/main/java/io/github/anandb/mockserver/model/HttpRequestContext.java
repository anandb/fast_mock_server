package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Context object containing parsed HTTP request data for Freemarker template evaluation.
 * <p>
 * This model is used to pass HTTP request information (headers, body, cookies, path variables) as data
 * to Freemarker templates when processing dynamic response bodies.
 * </p>
 */
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


    public static class HttpRequestContextBuilder {
        private Map<String, String> headers;
        private JsonNode body;
        private Map<String, String> cookies;
        private Map<String, String> pathVariables;

        HttpRequestContextBuilder() {
        }

        /**
         * Map of HTTP headers from the request.
         * Key: header name (String)
         * Value: header value (String, assumes only one value per header)
         * @return {@code this}.
         */
        public HttpRequestContext.HttpRequestContextBuilder headers(final Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Parsed JSON body from the request.
         * A JsonNode which represents the JSON structure.
         * @return {@code this}.
         */
        public HttpRequestContext.HttpRequestContextBuilder body(final JsonNode body) {
            this.body = body;
            return this;
        }

        /**
         * Map of cookies from the request.
         * Key: cookie name (String)
         * Value: cookie value (String)
         * @return {@code this}.
         */
        public HttpRequestContext.HttpRequestContextBuilder cookies(final Map<String, String> cookies) {
            this.cookies = cookies;
            return this;
        }

        /**
         * Map of path variables from the request path.
         * Key: path variable name (String)
         * Value: path variable value (String)
         * @return {@code this}.
         */
        public HttpRequestContext.HttpRequestContextBuilder pathVariables(final Map<String, String> pathVariables) {
            this.pathVariables = pathVariables;
            return this;
        }

        public HttpRequestContext build() {
            return new HttpRequestContext(this.headers, this.body, this.cookies, this.pathVariables);
        }

        @Override
        public String toString() {
            return "HttpRequestContext.HttpRequestContextBuilder(headers=" + this.headers + ", body=" + this.body + ", cookies=" + this.cookies + ", pathVariables=" + this.pathVariables + ")";
        }
    }

    public static HttpRequestContext.HttpRequestContextBuilder builder() {
        return new HttpRequestContext.HttpRequestContextBuilder();
    }

    /**
     * Map of HTTP headers from the request.
     * Key: header name (String)
     * Value: header value (String, assumes only one value per header)
     */
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * Parsed JSON body from the request.
     * A JsonNode which represents the JSON structure.
     */
    public JsonNode getBody() {
        return this.body;
    }

    /**
     * Map of cookies from the request.
     * Key: cookie name (String)
     * Value: cookie value (String)
     */
    public Map<String, String> getCookies() {
        return this.cookies;
    }

    /**
     * Map of path variables from the request path.
     * Key: path variable name (String)
     * Value: path variable value (String)
     */
    public Map<String, String> getPathVariables() {
        return this.pathVariables;
    }

    /**
     * Map of HTTP headers from the request.
     * Key: header name (String)
     * Value: header value (String, assumes only one value per header)
     */
    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Parsed JSON body from the request.
     * A JsonNode which represents the JSON structure.
     */
    public void setBody(final JsonNode body) {
        this.body = body;
    }

    /**
     * Map of cookies from the request.
     * Key: cookie name (String)
     * Value: cookie value (String)
     */
    public void setCookies(final Map<String, String> cookies) {
        this.cookies = cookies;
    }

    /**
     * Map of path variables from the request path.
     * Key: path variable name (String)
     * Value: path variable value (String)
     */
    public void setPathVariables(final Map<String, String> pathVariables) {
        this.pathVariables = pathVariables;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HttpRequestContext)) return false;
        final HttpRequestContext other = (HttpRequestContext) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$headers = this.getHeaders();
        final Object other$headers = other.getHeaders();
        if (this$headers == null ? other$headers != null : !this$headers.equals(other$headers)) return false;
        final Object this$body = this.getBody();
        final Object other$body = other.getBody();
        if (this$body == null ? other$body != null : !this$body.equals(other$body)) return false;
        final Object this$cookies = this.getCookies();
        final Object other$cookies = other.getCookies();
        if (this$cookies == null ? other$cookies != null : !this$cookies.equals(other$cookies)) return false;
        final Object this$pathVariables = this.getPathVariables();
        final Object other$pathVariables = other.getPathVariables();
        if (this$pathVariables == null ? other$pathVariables != null : !this$pathVariables.equals(other$pathVariables)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HttpRequestContext;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $headers = this.getHeaders();
        result = result * PRIME + ($headers == null ? 43 : $headers.hashCode());
        final Object $body = this.getBody();
        result = result * PRIME + ($body == null ? 43 : $body.hashCode());
        final Object $cookies = this.getCookies();
        result = result * PRIME + ($cookies == null ? 43 : $cookies.hashCode());
        final Object $pathVariables = this.getPathVariables();
        result = result * PRIME + ($pathVariables == null ? 43 : $pathVariables.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "HttpRequestContext(headers=" + this.getHeaders() + ", body=" + this.getBody() + ", cookies=" + this.getCookies() + ", pathVariables=" + this.getPathVariables() + ")";
    }

    public HttpRequestContext() {
    }

    /**
     * Creates a new {@code HttpRequestContext} instance.
     *
     * @param headers Map of HTTP headers from the request.
     * Key: header name (String)
     * Value: header value (String, assumes only one value per header)
     * @param body Parsed JSON body from the request.
     * A JsonNode which represents the JSON structure.
     * @param cookies Map of cookies from the request.
     * Key: cookie name (String)
     * Value: cookie value (String)
     * @param pathVariables Map of path variables from the request path.
     * Key: path variable name (String)
     * Value: path variable value (String)
     */
    public HttpRequestContext(final Map<String, String> headers, final JsonNode body, final Map<String, String> cookies, final Map<String, String> pathVariables) {
        this.headers = headers;
        this.body = body;
        this.cookies = cookies;
        this.pathVariables = pathVariables;
    }
}
