package com.example.mockserver.service;

import com.example.mockserver.exception.ServerAlreadyExistsException;
import com.example.mockserver.exception.ServerCreationException;
import com.example.mockserver.exception.ServerNotFoundException;
import com.example.mockserver.model.CreateServerRequest;
import com.example.mockserver.model.GlobalHeader;
import com.example.mockserver.model.ServerInfo;
import com.example.mockserver.model.TlsConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for MockServerManager.
 * <p>
 * Tests server lifecycle management including creation, retrieval, listing,
 * deletion, and error handling scenarios.
 * </p>
 */
@DisplayName("MockServerManager Tests")
class MockServerManagerTest {

    @Mock
    private TlsConfigurationService tlsConfigService;

    private MockServerManager mockServerManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockServerManager = new MockServerManager(tlsConfigService);
    }

    // Server Creation Tests

    @Test
    @DisplayName("Should create HTTP server successfully")
    void testCreateHttpServer() {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9001,
            "Test HTTP Server",
            null,
            null,
            null
        );

        ServerInfo serverInfo = mockServerManager.createServer(request);

        assertNotNull(serverInfo);
        assertEquals("test-server", serverInfo.getServerId());
        assertEquals(9001, serverInfo.getPort());
        assertEquals("http", serverInfo.getProtocol());
        assertEquals("http://localhost:9001", serverInfo.getBaseUrl());
        assertEquals("running", serverInfo.getStatus());
        assertFalse(serverInfo.isTlsEnabled());
        assertFalse(serverInfo.isMtlsEnabled());
    }

    @Test
    @DisplayName("Should create server with global headers")
    void testCreateServerWithGlobalHeaders() {
        List<GlobalHeader> headers = new ArrayList<>();
        headers.add(new GlobalHeader("X-Custom-Header", "CustomValue"));
        headers.add(new GlobalHeader("X-Environment", "test"));

        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9002,
            "Test Server with Headers",
            null,
            headers,
            null
        );

        ServerInfo serverInfo = mockServerManager.createServer(request);

        assertNotNull(serverInfo);
        assertNotNull(serverInfo.getGlobalHeaders());
        assertEquals(2, serverInfo.getGlobalHeaders().size());
        assertEquals("X-Custom-Header", serverInfo.getGlobalHeaders().get(0).getName());
        assertEquals("CustomValue", serverInfo.getGlobalHeaders().get(0).getValue());
    }

    @Test
    @DisplayName("Should reject duplicate server ID")
    void testCreateDuplicateServer() {
        CreateServerRequest request1 = new CreateServerRequest(
            "test-server",
            9003,
            "First Server",
            null,
            null,
            null
        );

        mockServerManager.createServer(request1);

        CreateServerRequest request2 = new CreateServerRequest(
            "test-server",
            9004,
            "Duplicate Server",
            null,
            null,
            null
        );

        assertThrows(
            ServerAlreadyExistsException.class,
            () -> mockServerManager.createServer(request2)
        );
    }

    @Test
    @DisplayName("Should configure TLS when TLS config provided")
    void testCreateServerWithTls() throws Exception {
        TlsConfig tlsConfig = new TlsConfig();
        tlsConfig.setCertificate("cert-content");
        tlsConfig.setPrivateKey("key-content");

        CreateServerRequest request = new CreateServerRequest(
            "tls-server",
            9005,
            "TLS Server",
            tlsConfig,
            null,
            null
        );

        doNothing().when(tlsConfigService).configureTls(anyString(), any(TlsConfig.class));

        ServerInfo serverInfo = mockServerManager.createServer(request);

        assertNotNull(serverInfo);
        verify(tlsConfigService).configureTls(eq("tls-server"), eq(tlsConfig));
    }

    @Test
    @DisplayName("Should handle TLS configuration failure")
    void testCreateServerWithTlsFailure() throws Exception {
        TlsConfig tlsConfig = new TlsConfig();
        tlsConfig.setCertificate("invalid-cert");
        tlsConfig.setPrivateKey("invalid-key");

        CreateServerRequest request = new CreateServerRequest(
            "tls-server",
            9006,
            "TLS Server",
            tlsConfig,
            null,
            null
        );

        doThrow(new RuntimeException("TLS configuration failed"))
            .when(tlsConfigService).configureTls(anyString(), any(TlsConfig.class));

        assertThrows(
            ServerCreationException.class,
            () -> mockServerManager.createServer(request)
        );
    }

    // Server Retrieval Tests

    @Test
    @DisplayName("Should retrieve server info successfully")
    void testGetServerInfo() {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9007,
            "Test Server",
            null,
            null,
            null
        );

        mockServerManager.createServer(request);
        ServerInfo serverInfo = mockServerManager.getServerInfo("test-server");

        assertNotNull(serverInfo);
        assertEquals("test-server", serverInfo.getServerId());
        assertEquals(9007, serverInfo.getPort());
    }

    @Test
    @DisplayName("Should throw exception when server not found")
    void testGetServerInfoNotFound() {
        assertThrows(
            ServerNotFoundException.class,
            () -> mockServerManager.getServerInfo("non-existent")
        );
    }

    @Test
    @DisplayName("Should retrieve server instance successfully")
    void testGetServerInstance() {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9008,
            "Test Server",
            null,
            null,
            null
        );

        mockServerManager.createServer(request);
        MockServerManager.ServerInstance instance = mockServerManager.getServerInstance("test-server");

        assertNotNull(instance);
        assertEquals("test-server", instance.getServerId());
        assertEquals(9008, instance.getPort());
        assertNotNull(instance.getServer());
    }

    // Server Listing Tests

    @Test
    @DisplayName("Should list all servers")
    void testListServers() {
        CreateServerRequest request1 = new CreateServerRequest("server1", 9009, "Server 1", null, null, null);
        CreateServerRequest request2 = new CreateServerRequest("server2", 9010, "Server 2", null, null, null);
        CreateServerRequest request3 = new CreateServerRequest("server3", 9011, "Server 3", null, null, null);

        mockServerManager.createServer(request1);
        mockServerManager.createServer(request2);
        mockServerManager.createServer(request3);

        List<ServerInfo> servers = mockServerManager.listServers();

        assertNotNull(servers);
        assertEquals(3, servers.size());

        assertTrue(servers.stream().anyMatch(s -> s.getServerId().equals("server1")));
        assertTrue(servers.stream().anyMatch(s -> s.getServerId().equals("server2")));
        assertTrue(servers.stream().anyMatch(s -> s.getServerId().equals("server3")));
    }

    @Test
    @DisplayName("Should return empty list when no servers exist")
    void testListServersEmpty() {
        List<ServerInfo> servers = mockServerManager.listServers();

        assertNotNull(servers);
        assertTrue(servers.isEmpty());
    }

    // Server Deletion Tests

    @Test
    @DisplayName("Should delete server successfully")
    void testDeleteServer() {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9012,
            "Test Server",
            null,
            null,
            null
        );

        mockServerManager.createServer(request);
        assertTrue(mockServerManager.serverExists("test-server"));

        boolean deleted = mockServerManager.deleteServer("test-server");

        assertTrue(deleted);
        assertFalse(mockServerManager.serverExists("test-server"));
        verify(tlsConfigService).cleanupServerCertificates("test-server");
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent server")
    void testDeleteNonExistentServer() {
        assertThrows(
            ServerNotFoundException.class,
            () -> mockServerManager.deleteServer("non-existent")
        );
    }

    @Test
    @DisplayName("Should cleanup certificates when deleting server")
    void testDeleteServerCleanupCertificates() {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9013,
            "Test Server",
            null,
            null,
            null
        );

        mockServerManager.createServer(request);
        mockServerManager.deleteServer("test-server");

        verify(tlsConfigService).cleanupServerCertificates("test-server");
    }

    // Server Existence Tests

    @Test
    @DisplayName("Should return true when server exists")
    void testServerExists() {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9014,
            "Test Server",
            null,
            null,
            null
        );

        mockServerManager.createServer(request);

        assertTrue(mockServerManager.serverExists("test-server"));
    }

    @Test
    @DisplayName("Should return false when server does not exist")
    void testServerDoesNotExist() {
        assertFalse(mockServerManager.serverExists("non-existent"));
    }

    // Server Count Tests

    @Test
    @DisplayName("Should return correct server count")
    void testGetServerCount() {
        assertEquals(0, mockServerManager.getServerCount());

        mockServerManager.createServer(new CreateServerRequest("server1", 9015, "Server 1", null, null, null));
        assertEquals(1, mockServerManager.getServerCount());

        mockServerManager.createServer(new CreateServerRequest("server2", 9016, "Server 2", null, null, null));
        assertEquals(2, mockServerManager.getServerCount());

        mockServerManager.deleteServer("server1");
        assertEquals(1, mockServerManager.getServerCount());
    }

    // Shutdown Tests

    @Test
    @DisplayName("Should shutdown all servers on service shutdown")
    void testShutdown() {
        mockServerManager.createServer(new CreateServerRequest("server1", 9017, "Server 1", null, null, null));
        mockServerManager.createServer(new CreateServerRequest("server2", 9018, "Server 2", null, null, null));

        assertEquals(2, mockServerManager.getServerCount());

        mockServerManager.shutdown();

        assertEquals(0, mockServerManager.getServerCount());
    }

    // ServerInstance Tests

    @Test
    @DisplayName("Should determine protocol correctly for HTTP")
    void testServerInstanceProtocolHttp() {
        CreateServerRequest request = new CreateServerRequest(
            "http-server",
            9019,
            "HTTP Server",
            null,
            null,
            null
        );

        mockServerManager.createServer(request);
        MockServerManager.ServerInstance instance = mockServerManager.getServerInstance("http-server");

        assertEquals("http", instance.getProtocol());
        assertEquals("http://localhost:9019", instance.getBaseUrl());
        assertFalse(instance.isTlsEnabled());
    }

    @Test
    @DisplayName("Should check for global headers correctly")
    void testServerInstanceHasGlobalHeaders() {
        List<GlobalHeader> headers = new ArrayList<>();
        headers.add(new GlobalHeader("X-Header", "value"));

        CreateServerRequest request = new CreateServerRequest(
            "server-with-headers",
            9020,
            "Server with Headers",
            null,
            headers,
            null
        );

        mockServerManager.createServer(request);
        MockServerManager.ServerInstance instance = mockServerManager.getServerInstance("server-with-headers");

        assertTrue(instance.hasGlobalHeaders());
        assertEquals(1, instance.getGlobalHeaders().size());
    }

    @Test
    @DisplayName("Should handle server without global headers")
    void testServerInstanceWithoutGlobalHeaders() {
        CreateServerRequest request = new CreateServerRequest(
            "server-no-headers",
            9021,
            "Server without Headers",
            null,
            null,
            null
        );

        mockServerManager.createServer(request);
        MockServerManager.ServerInstance instance = mockServerManager.getServerInstance("server-no-headers");

        assertFalse(instance.hasGlobalHeaders());
    }

    @Test
    @DisplayName("Should include description in server info")
    void testServerInfoWithDescription() {
        CreateServerRequest request = new CreateServerRequest(
            "described-server",
            9022,
            "This is a detailed description",
            null,
            null,
            null
        );

        ServerInfo serverInfo = mockServerManager.createServer(request);

        assertEquals("This is a detailed description", serverInfo.getDescription());
    }

    @Test
    @DisplayName("Should handle null description")
    void testServerInfoWithNullDescription() {
        CreateServerRequest request = new CreateServerRequest(
            "no-description-server",
            9023,
            null,
            null,
            null,
            null
        );

        ServerInfo serverInfo = mockServerManager.createServer(request);

        assertNull(serverInfo.getDescription());
    }
}
