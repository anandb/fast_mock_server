package io.github.anandb.mockserver.util;

import io.github.anandb.mockserver.model.GlobalHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResponseUtils Tests")
class ResponseUtilsTest {

    @Test
    void mergeGlobalHeadersAddsNewHeaders() {
        HttpResponse response = HttpResponse.response()
                .withStatusCode(200);
        List<GlobalHeader> globalHeaders = List.of(
                new GlobalHeader("X-Custom", "value1"),
                new GlobalHeader("X-Another", "value2")
        );

        HttpResponse result = ResponseUtils.mergeGlobalHeaders(response, globalHeaders);

        assertEquals("value1", result.getFirstHeader("X-Custom"));
        assertEquals("value2", result.getFirstHeader("X-Another"));
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void mergeGlobalHeadersDoesNotOverrideExisting() {
        HttpResponse response = HttpResponse.response()
                .withStatusCode(200)
                .withHeader("X-Custom", "original");
        List<GlobalHeader> globalHeaders = List.of(
                new GlobalHeader("X-Custom", "should-not-override")
        );

        HttpResponse result = ResponseUtils.mergeGlobalHeaders(response, globalHeaders);

        assertEquals("original", result.getFirstHeader("X-Custom"));
    }

    @Test
    void mergeGlobalHeadersReturnsOriginalWhenNull() {
        HttpResponse response = HttpResponse.response().withStatusCode(200);

        HttpResponse result = ResponseUtils.mergeGlobalHeaders(response, null);

        assertSame(response, result);
    }

    @Test
    void mergeGlobalHeadersReturnsOriginalWhenEmpty() {
        HttpResponse response = HttpResponse.response().withStatusCode(200);

        HttpResponse result = ResponseUtils.mergeGlobalHeaders(response, List.of());

        assertSame(response, result);
    }

    @Test
    void mergeGlobalHeadersPreservesExistingResponseHeaders() {
        HttpResponse response = HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json");
        List<GlobalHeader> globalHeaders = List.of(
                new GlobalHeader("X-Custom", "value")
        );

        HttpResponse result = ResponseUtils.mergeGlobalHeaders(response, globalHeaders);

        assertEquals("application/json", result.getFirstHeader("Content-Type"));
        assertEquals("value", result.getFirstHeader("X-Custom"));
    }
}
