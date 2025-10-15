package io.github.anandb.mockserver.controller;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.service.MockServerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ServerController.
 * <p>
 * Tests REST API endpoints for server lifecycle management using MockMvc
 * to simulate HTTP requests without starting the full application.
 * </p>
 */
@WebMvcTest(ServerController.class)
@DisplayName("ServerController Tests")
class ServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MockServerManager mockServerManager;

    // Create Server Tests

    @Test
    @DisplayName("Should create server and return 201 Created")
    void testCreateServer() throws Exception {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            8080,
            "Test Server",
            null,
            null,
            null,
            null
        );

        ServerInfo serverInfo = ServerInfo.builder()
            .serverId("test-server")
            .port(8080)
            .description("Test Server")
            .protocol("http")
            .baseUrl("http://localhost:8080")
            .tlsEnabled(false)
            .mtlsEnabled(false)
            .createdAt(LocalDateTime.now())
            .status("running")
            .build();

        when(mockServerManager.createServer(any(CreateServerRequest.class)))
            .thenReturn(serverInfo);

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.serverId").value("test-server"))
            .andExpect(jsonPath("$.port").value(8080))
            .andExpect(jsonPath("$.protocol").value("http"))
            .andExpect(jsonPath("$.baseUrl").value("http://localhost:8080"))
            .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    @DisplayName("Should return 409 Conflict for duplicate server ID")
    void testCreateDuplicateServer() throws Exception {
        CreateServerRequest request = new CreateServerRequest(
            "existing-server",
            8080,
            "Test Server",
            null,
            null,
            null,
            null
        );

        when(mockServerManager.createServer(any(CreateServerRequest.class)))
            .thenThrow(new ServerAlreadyExistsException("existing-server"));

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("SERVER_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid port")
    void testCreateServerWithInvalidPort() throws Exception {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            100,  // Invalid port (< 1024)
            "Test Server",
            null,
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for missing serverId")
    void testCreateServerWithoutServerId() throws Exception {
        CreateServerRequest request = new CreateServerRequest(
            null,  // Missing serverId
            8080,
            "Test Server",
            null,
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for port exceeding maximum")
    void testCreateServerWithPortTooHigh() throws Exception {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            70000,  // Port exceeds 65535
            "Test Server",
            null,
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // List Servers Tests

    @Test
    @DisplayName("Should list all servers and return 200 OK")
    void testListServers() throws Exception {
        ServerInfo server1 = ServerInfo.builder()
            .serverId("server1")
            .port(8080)
            .protocol("http")
            .baseUrl("http://localhost:8080")
            .status("running")
            .build();

        ServerInfo server2 = ServerInfo.builder()
            .serverId("server2")
            .port(8443)
            .protocol("https")
            .baseUrl("https://localhost:8443")
            .status("running")
            .build();

        List<ServerInfo> servers = Arrays.asList(server1, server2);

        when(mockServerManager.listServers()).thenReturn(servers);

        mockMvc.perform(get("/api/servers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].serverId").value("server1"))
            .andExpect(jsonPath("$[1].serverId").value("server2"));
    }

    @Test
    @DisplayName("Should return empty list when no servers exist")
    void testListServersEmpty() throws Exception {
        when(mockServerManager.listServers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/servers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    // Get Server Tests

    @Test
    @DisplayName("Should get server by ID and return 200 OK")
    void testGetServer() throws Exception {
        ServerInfo serverInfo = ServerInfo.builder()
            .serverId("test-server")
            .port(8080)
            .description("Test Server")
            .protocol("http")
            .baseUrl("http://localhost:8080")
            .status("running")
            .build();

        when(mockServerManager.getServerInfo("test-server")).thenReturn(serverInfo);

        mockMvc.perform(get("/api/servers/test-server"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.serverId").value("test-server"))
            .andExpect(jsonPath("$.port").value(8080))
            .andExpect(jsonPath("$.description").value("Test Server"));
    }

    @Test
    @DisplayName("Should return 404 Not Found for non-existent server")
    void testGetNonExistentServer() throws Exception {
        when(mockServerManager.getServerInfo("non-existent"))
            .thenThrow(new ServerNotFoundException("non-existent"));

        mockMvc.perform(get("/api/servers/non-existent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("SERVER_NOT_FOUND"));
    }

    // Delete Server Tests

    @Test
    @DisplayName("Should delete server and return 204 No Content")
    void testDeleteServer() throws Exception {
        when(mockServerManager.deleteServer("test-server")).thenReturn(true);

        mockMvc.perform(delete("/api/servers/test-server"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 Not Found when deleting non-existent server")
    void testDeleteNonExistentServer() throws Exception {
        when(mockServerManager.deleteServer("non-existent"))
            .thenThrow(new ServerNotFoundException("non-existent"));

        mockMvc.perform(delete("/api/servers/non-existent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("SERVER_NOT_FOUND"));
    }

    // Server Exists Tests

    @Test
    @DisplayName("Should return true when server exists")
    void testServerExists() throws Exception {
        when(mockServerManager.serverExists("test-server")).thenReturn(true);

        mockMvc.perform(get("/api/servers/test-server/exists"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Should return false when server does not exist")
    void testServerDoesNotExist() throws Exception {
        when(mockServerManager.serverExists("non-existent")).thenReturn(false);

        mockMvc.perform(get("/api/servers/non-existent/exists"))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    // Request Validation Tests

    @Test
    @DisplayName("Should validate empty serverId")
    void testCreateServerWithEmptyServerId() throws Exception {
        CreateServerRequest request = new CreateServerRequest(
            "",  // Empty serverId
            8080,
            "Test Server",
            null,
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate missing port")
    void testCreateServerWithoutPort() throws Exception {
        String requestJson = """
            {
                "serverId": "test-server",
                "description": "Test Server"
            }
            """;

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should accept valid server with all optional fields")
    void testCreateServerWithAllFields() throws Exception {
        String requestJson = """
            {
                "serverId": "full-server",
                "port": 8080,
                "description": "Fully configured server",
                "tlsConfig": null,
                "globalHeaders": []
            }
            """;

        ServerInfo serverInfo = ServerInfo.builder()
            .serverId("full-server")
            .port(8080)
            .description("Fully configured server")
            .protocol("http")
            .baseUrl("http://localhost:8080")
            .status("running")
            .build();

        when(mockServerManager.createServer(any(CreateServerRequest.class)))
            .thenReturn(serverInfo);

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.serverId").value("full-server"));
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testCreateServerWithMalformedJson() throws Exception {
        String malformedJson = "{ invalid json }";

        mockMvc.perform(post("/api/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest());
    }
}
