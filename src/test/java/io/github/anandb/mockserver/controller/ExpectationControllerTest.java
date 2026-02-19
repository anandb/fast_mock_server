package io.github.anandb.mockserver.controller;

import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.ServerInstance;
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

    @MockitoBean
    private io.github.anandb.mockserver.strategy.ResponseStrategy responseStrategy;

    @Mock
    private org.mockserver.integration.ClientAndServer mockClientAndServer;

    @Mock
    private org.mockserver.client.ForwardChainExpectation mockForwardChainExpectation;

    @BeforeEach
    void setUp() {
        doNothing().when(mockServerOperations).configureExpectation(any(), any());
        doNothing().when(mockServerOperations).reset();
        lenient().when(mockServerOperations.retrieveActiveExpectations(any())).thenReturn(new Expectation[0]);

        when(mockClientAndServer.when(any())).thenReturn(mockForwardChainExpectation);
        when(mockClientAndServer.reset()).thenReturn(mockClientAndServer);
        lenient().when(mockClientAndServer.retrieveActiveExpectations(any())).thenReturn(new Expectation[0]);
    }

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

        ServerInstance serverInstance = createMockServerInstance("test-server", null);
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

        ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(post("/api/servers/test-server/expectations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expectationsJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Successfully configured")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2 expectation")));
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
    @DisplayName("Should retrieve all expectations successfully")
    void testGetExpectations() throws Exception {
        ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(get("/api/servers/test-server/expectations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should clear all expectations successfully")
    void testClearExpectations() throws Exception {
        ServerInstance serverInstance = createMockServerInstance("test-server", null);
        when(mockServerManager.getServerInstance("test-server")).thenReturn(serverInstance);

        mockMvc.perform(delete("/api/servers/test-server/expectations"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cleared all expectations")));
    }

    private ServerInstance createMockServerInstance(
            String serverId,
            List<GlobalHeader> globalHeaders) {
        return new ServerInstance(
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
