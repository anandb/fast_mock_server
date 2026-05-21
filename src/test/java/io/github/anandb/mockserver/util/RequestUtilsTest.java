package io.github.anandb.mockserver.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.NottableString;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RequestUtils Tests")
class RequestUtilsTest {

    @Test
    void extractPathVariablesReturnsEmptyForNullPattern() {
        assertTrue(RequestUtils.extractPathVariables("/test", null).isEmpty());
    }

    @Test
    void extractPathVariablesReturnsEmptyForNullPath() {
        assertTrue(RequestUtils.extractPathVariables(null, "/test").isEmpty());
    }

    @Test
    void extractPathVariablesReturnsEmptyForMismatchedLength() {
        assertTrue(RequestUtils.extractPathVariables("/a/b", "/a").isEmpty());
    }

    @Test
    void extractPathVariablesFindsSimpleVariable() {
        Map<String, String> vars = RequestUtils.extractPathVariables("/users/123", "/users/{id}");
        assertEquals(1, vars.size());
        assertEquals("123", vars.get("id"));
    }

    @Test
    void extractPathVariablesFindsMultipleVariables() {
        Map<String, String> vars = RequestUtils.extractPathVariables("/users/123/posts/456", "/users/{userId}/posts/{postId}");
        assertEquals(2, vars.size());
        assertEquals("123", vars.get("userId"));
        assertEquals("456", vars.get("postId"));
    }

    @Test
    void extractPathVariablesReturnsEmptyForNoVariables() {
        Map<String, String> vars = RequestUtils.extractPathVariables("/api/health", "/api/health");
        assertTrue(vars.isEmpty());
    }

    @Test
    void headersToMapReturnsEmptyForNullHeaders() {
        HttpRequest request = HttpRequest.request();
        assertTrue(RequestUtils.headersToMap(request).isEmpty());
    }

    @Test
    void headersToMapConvertsHeaders() {
        HttpRequest request = HttpRequest.request()
                .withHeader(new Header(NottableString.string("Content-Type"), NottableString.string("application/json")))
                .withHeader(new Header(NottableString.string("Accept"), NottableString.string("text/plain")));

        Map<String, List<String>> result = RequestUtils.headersToMap(request);

        assertEquals(2, result.size());
        assertEquals("application/json", result.get("Content-Type").get(0));
        assertEquals("text/plain", result.get("Accept").get(0));
    }

    @Test
    void headersToMapHandlesDuplicateHeaders() {
        HttpRequest request = HttpRequest.request()
                .withHeader(new Header(NottableString.string("X-Custom"), NottableString.string("val1")))
                .withHeader(new Header(NottableString.string("X-Custom"), NottableString.string("val2")));

        Map<String, List<String>> result = RequestUtils.headersToMap(request);

        assertEquals(1, result.size());
        assertTrue(result.get("X-Custom").contains("val1"));
    }
}
