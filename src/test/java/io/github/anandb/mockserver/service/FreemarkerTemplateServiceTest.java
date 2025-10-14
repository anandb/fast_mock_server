package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.HttpRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Cookie;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.NottableString.string;

/**
 * Unit tests for FreemarkerTemplateService, particularly focusing on path variable extraction.
 */
class FreemarkerTemplateServiceTest {

    private FreemarkerTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new FreemarkerTemplateService();
    }

    @Test
    void testExtractPathVariables_SimpleVariable() {
        // Given
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getPathVariables());
        assertEquals(1, context.getPathVariables().size());
        assertEquals("123", context.getPathVariables().get("id"));
    }

    @Test
    void testExtractPathVariables_MultipleVariables() {
        // Given
        String pathPattern = "/users/{userId}/posts/{postId}";
        String requestPath = "/users/123/posts/456";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getPathVariables());
        assertEquals(2, context.getPathVariables().size());
        assertEquals("123", context.getPathVariables().get("userId"));
        assertEquals("456", context.getPathVariables().get("postId"));
    }

    @Test
    void testExtractPathVariables_MixedPathWithVariables() {
        // Given
        String pathPattern = "/api/v1/users/{id}/profile";
        String requestPath = "/api/v1/users/abc123/profile";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getPathVariables());
        assertEquals(1, context.getPathVariables().size());
        assertEquals("abc123", context.getPathVariables().get("id"));
    }

    @Test
    void testExtractPathVariables_NoVariables() {
        // Given
        String pathPattern = "/api/users/list";
        String requestPath = "/api/users/list";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getPathVariables());
        assertEquals(0, context.getPathVariables().size());
    }

    @Test
    void testExtractPathVariables_NullPathPattern() {
        // Given
        String requestPath = "/users/123";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, null);

        // Then
        assertNotNull(context.getPathVariables());
        assertEquals(0, context.getPathVariables().size());
    }

    @Test
    void testExtractPathVariables_MismatchedSegmentCount() {
        // Given
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123/extra";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getPathVariables());
        assertEquals(0, context.getPathVariables().size());
    }

    @Test
    void testParseHttpRequest_WithHeadersAndPathVariables() {
        // Given
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath)
                .withHeader(Header.header(string("X-User-Name"), string("John Doe")))
                .withHeader(Header.header(string("Authorization"), string("Bearer token123")));

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getHeaders());
        assertEquals(2, context.getHeaders().size());
        assertEquals("John Doe", context.getHeaders().get("X-User-Name"));
        assertEquals("Bearer token123", context.getHeaders().get("Authorization"));

        assertNotNull(context.getPathVariables());
        assertEquals(1, context.getPathVariables().size());
        assertEquals("123", context.getPathVariables().get("id"));
    }

    @Test
    void testParseHttpRequest_WithBodyAndPathVariables() {
        // Given
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123";
        String jsonBody = "{\"name\": \"John\", \"age\": 30}";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath)
                .withBody(jsonBody);

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getBody());
        assertEquals("John", context.getBody().get("name").asText());
        assertEquals(30, context.getBody().get("age").asInt());

        assertNotNull(context.getPathVariables());
        assertEquals(1, context.getPathVariables().size());
        assertEquals("123", context.getPathVariables().get("id"));
    }

    @Test
    void testParseHttpRequest_WithCookiesAndPathVariables() {
        // Given
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath)
                .withCookie(Cookie.cookie(string("sessionId"), string("abc123")))
                .withCookie(Cookie.cookie(string("userId"), string("user456")));

        // When
        HttpRequestContext context = templateService.parseHttpRequest(httpRequest, pathPattern);

        // Then
        assertNotNull(context.getCookies());
        assertEquals(2, context.getCookies().size());
        assertEquals("abc123", context.getCookies().get("sessionId"));
        assertEquals("user456", context.getCookies().get("userId"));

        assertNotNull(context.getPathVariables());
        assertEquals(1, context.getPathVariables().size());
        assertEquals("123", context.getPathVariables().get("id"));
    }

    @Test
    void testProcessTemplate_WithPathVariables() throws Exception {
        // Given
        String template = "User ID: ${pathVariables.id}";
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        String result = templateService.processTemplateWithRequest(template, httpRequest, pathPattern);

        // Then
        assertEquals("User ID: 123", result);
    }

    @Test
    void testProcessTemplate_WithMultiplePathVariables() throws Exception {
        // Given
        String template = "User ${pathVariables.userId} posted ${pathVariables.postId}";
        String pathPattern = "/users/{userId}/posts/{postId}";
        String requestPath = "/users/123/posts/456";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath);

        // When
        String result = templateService.processTemplateWithRequest(template, httpRequest, pathPattern);

        // Then
        assertEquals("User 123 posted 456", result);
    }

    @Test
    void testProcessTemplate_WithHeadersAndPathVariables() throws Exception {
        // Given
        String template = "User ${pathVariables.id} with name ${headers['X-User-Name']}";
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath)
                .withHeader(Header.header(string("X-User-Name"), string("John")));

        // When
        String result = templateService.processTemplateWithRequest(template, httpRequest, pathPattern);

        // Then
        assertEquals("User 123 with name John", result);
    }

    @Test
    void testProcessTemplate_WithComplexJsonAndPathVariables() throws Exception {
        // Given
        String template = """
                {
                  "userId": "${pathVariables.id}",
                  "userName": "${headers['X-User-Name']}",
                  "requestData": {
                    "name": "${body.name}",
                    "age": ${body.age}
                  }
                }
                """;
        String pathPattern = "/users/{id}";
        String requestPath = "/users/123";
        String jsonBody = "{\"name\": \"John\", \"age\": 30}";
        HttpRequest httpRequest = HttpRequest.request()
                .withPath(requestPath)
                .withHeader(Header.header(string("X-User-Name"), string("JohnDoe")))
                .withBody(jsonBody);

        // When
        String result = templateService.processTemplateWithRequest(template, httpRequest, pathPattern);

        // Then
        assertTrue(result.contains("\"userId\": \"123\""));
        assertTrue(result.contains("\"userName\": \"JohnDoe\""));
        assertTrue(result.contains("\"name\": \"John\""));
        assertTrue(result.contains("\"age\": 30"));
    }
}
