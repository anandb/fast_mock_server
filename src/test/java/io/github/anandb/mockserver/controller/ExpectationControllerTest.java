package io.github.anandb.mockserver.controller;

import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.service.FreemarkerTemplateService;
import io.github.anandb.mockserver.service.MockServerManager;
import io.github.anandb.mockserver.service.MockServerOperations;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.mockserver.mock.Expectation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/**
 * Unit tests for ExpectationController.
 * <p>
 * Tests REST API endpoints for configuring and managing expectations
 * on mock servers, including global header merging functionality.
 * </p>
 */
@WebMvcTest(ExpectationController.class)
@DisplayName("ExpectationController Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpectationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MockServerManager mockServerManager;

    @MockitoBean
    private MockServerOperations mockServerOperations;

    @MockitoBean
    private FreemarkerTemplateService mockTemplateService;

    @Mock
    private org.mockserver.integration.ClientAndServer mockClientAndServer;

    @Mock
    private org.mockserver.client.ForwardChainExpectation mockForwardChainExpectation;

    @BeforeEach
    void setUp() {
        // Setup MockServerOperations mock
        doNothing().when(mockServerOperations).configureExpectation(any(), any());
        doNothing().when(mockServerOperations).reset();
        lenient().when(mockServerOperations.retrieveActiveExpectations(any())).thenReturn(new Expectation[0]);

        // Setup ClientAndServer mock for ServerInstance - this is the key fix
        when(mockClientAndServer.when(any())).thenReturn(mockForwardChainExpectation);
        when(mockClientAndServer.reset()).thenReturn(mockClientAndServer);
        lenient().when(mockClientAndServer.retrieveActiveExpectations(any())).thenReturn(new Expectation[0]);
    }

    // Configure Expectations Tests

    @Test
    @DisplayName("Should configure single expectation successfully")
    void testConfigureSingleExpectation() throws Exception {
        String expectationJson = """
                {
                    "httpRequest": {
                        "method": "GET",
                        "path": "/api/users/123"
                    },
                    "httpResponse": {
                        "statusCode": 200,
                        "body": {
                            "id": 123,
                            "name": "John Doe"
                        }
                    }
                }
                """;

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Successfully configured")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1 expectation")));
    }

    @Test
    @DisplayName("Should configure multiple expectations successfully")
    void testConfigureMultipleExpectations() throws Exception {
        String expectationsJson = """
                [
                    {
                        "httpRequest": {
                            "method": "GET",
                            "path": "/api/users"
                        },
                        "httpResponse": {
                            "statusCode": 200,
                            "body": {"users": []}
                        }
                    },
                    {
                        "httpRequest": {
                            "method": "POST",
                            "path": "/api/users"
                        },
                        "httpResponse": {
                            "statusCode": 201,
                            "body": {"id": 1}
                        }
                    }
                ]
                """;

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationsJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Successfully configured")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2 expectation")));
    }

    @Test
    @DisplayName("Should configure expectations with global headers")
    void testConfigureExpectationsWithGlobalHeaders() throws Exception {
        String expectationJson = """
                {
                    "httpRequest": {
                        "method": "GET",
                        "path": "/api/data"
                    },
                    "httpResponse": {
                        "statusCode": 200,
                        "body": {"data": "test"}
                    }
                }
                """;

        List<GlobalHeader> globalHeaders = Arrays.asList(
                new GlobalHeader("X-Service-Version", "1.0.0"),
                new GlobalHeader("X-Environment", "test"));

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", globalHeaders);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Successfully configured")));
    }

    @Test
    @DisplayName("Should return 404 when server not found")
    void testConfigureExpectationsServerNotFound() throws Exception {
        String expectationJson = """
                {
                    "httpRequest": {
                        "method": "GET",
                        "path": "/test"
                    },
                    "httpResponse": {
                        "statusCode": 200
                    }
                }
                """;

        when(mockServerManager.getServerInstance("non-existent"))
                .thenThrow(new ServerNotFoundException("non-existent"));

        mockMvc.perform(post("/api/servers/non-existent/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SERVER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return 400 for invalid expectation JSON")
    void testConfigureInvalidExpectation() throws Exception {
        String invalidJson = """
                {
                    "invalid": "structure"
                }
                """;

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_EXPECTATION"));
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testConfigureExpectationsWithMalformedJson() throws Exception {
        String malformedJson = "{ invalid json }";

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    // Get Expectations Tests

    @Test
    @DisplayName("Should retrieve all expectations successfully")
    void testGetExpectations() throws Exception {
        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(get("/api/servers/test-server/expectations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 404 when retrieving expectations for non-existent server")
    void testGetExpectationsServerNotFound() throws Exception {
        when(mockServerManager.getServerInstance("non-existent"))
                .thenThrow(new ServerNotFoundException("non-existent"));

        mockMvc.perform(get("/api/servers/non-existent/expectations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SERVER_NOT_FOUND"));
    }

    // Clear Expectations Tests

    @Test
    @DisplayName("Should clear all expectations successfully")
    void testClearExpectations() throws Exception {
        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(delete("/api/servers/test-server/expectations"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cleared all expectations")));
    }

    @Test
    @DisplayName("Should return 404 when clearing expectations for non-existent server")
    void testClearExpectationsServerNotFound() throws Exception {
        when(mockServerManager.getServerInstance("non-existent"))
                .thenThrow(new ServerNotFoundException("non-existent"));

        mockMvc.perform(delete("/api/servers/non-existent/expectations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SERVER_NOT_FOUND"));
    }

    // Complex Expectation Tests

    @Test
    @DisplayName("Should handle expectations with query parameters")
    void testConfigureExpectationWithQueryParams() throws Exception {
        String expectationJson = """
                {
                    "httpRequest": {
                        "method": "GET",
                        "path": "/api/search",
                        "queryStringParameters": {
                            "q": ["test"],
                            "page": ["1"]
                        }
                    },
                    "httpResponse": {
                        "statusCode": 200,
                        "body": {"results": []}
                    }
                }
                """;

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle expectations with request headers")
    void testConfigureExpectationWithRequestHeaders() throws Exception {
        String expectationJson = """
                {
                    "httpRequest": {
                        "method": "GET",
                        "path": "/api/protected",
                        "headers": {
                            "Authorization": ["Bearer token123"]
                        }
                    },
                    "httpResponse": {
                        "statusCode": 200,
                        "body": {"authorized": true}
                    }
                }
                """;

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle expectations with delay")
    void testConfigureExpectationWithDelay() throws Exception {
        String expectationJson = """
                {
                    "httpRequest": {
                        "method": "GET",
                        "path": "/api/slow"
                    },
                    "httpResponse": {
                        "statusCode": 200,
                        "body": {"message": "Slow response"},
                        "delay": {
                            "timeUnit": "SECONDS",
                            "value": 2
                        }
                    }
                }
                """;

        MockServerManager.ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationJson))
                .andExpect(status().isOk());
    }

    // Helper Methods

    private MockServerManager.ServerInstance createMockServerInstance(
            String serverId,
            List<GlobalHeader> globalHeaders) {
        return new MockServerManager.ServerInstance(
                serverId,
                8080,
                mockClientAndServer,
                null,
                globalHeaders,
                null,
                null,
                LocalDateTime.now(),
                "Test Server");
    }
}
