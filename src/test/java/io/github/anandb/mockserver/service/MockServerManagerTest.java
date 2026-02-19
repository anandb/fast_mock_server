package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.model.TlsConfig;
import io.github.anandb.mockserver.strategy.ResponseStrategy;

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
 */
@DisplayName("MockServerManager Tests")
class MockServerManagerTest {

    @Mock
    private TlsConfigurationService tlsConfigService;

    @Mock
    private RelayService relayService;

    private List<ResponseStrategy> strategies = new ArrayList<>();

    private MockServerManager mockServerManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockServerManager = new MockServerManager(tlsConfigService, relayService, strategies);
    }

    @Test
    @DisplayName("Should create HTTP server successfully")
    void testCreateHttpServer() {
        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9001,
            "Test HTTP Server",
            null,
            null,
            null,
            null
        );

        ServerInfo serverInfo = mockServerManager.createServer(request);

        assertNotNull(serverInfo);
        assertEquals("test-server", serverInfo.getServerId());
        assertEquals(9001, serverInfo.getPort());
        assertEquals("http", serverInfo.getProtocol());
        assertEquals("running", serverInfo.getStatus());
    }

    @Test
    @DisplayName("Should create server with global headers")
    void testCreateServerWithGlobalHeaders() {
        List<GlobalHeader> headers = new ArrayList<>();
        headers.add(new GlobalHeader("X-Custom-Header", "CustomValue"));

        CreateServerRequest request = new CreateServerRequest(
            "test-server",
            9002,
            "Test Server with Headers",
            null,
            headers,
            null,
            null
        );

        ServerInfo serverInfo = mockServerManager.createServer(request);

        assertNotNull(serverInfo);
        assertNotNull(serverInfo.getGlobalHeaders());
        assertEquals(1, serverInfo.getGlobalHeaders().size());
    }

    @Test
    @DisplayName("Should reject duplicate server ID")
    void testCreateDuplicateServer() {
        CreateServerRequest request1 = new CreateServerRequest("test-server", 9003, "First", null, null, null, null);
        mockServerManager.createServer(request1);

        CreateServerRequest request2 = new CreateServerRequest("test-server", 9004, "Duplicate", null, null, null, null);

        assertThrows(ServerAlreadyExistsException.class, () -> mockServerManager.createServer(request2));
    }

    @Test
    @DisplayName("Should configure TLS when TLS config provided")
    void testCreateServerWithTls() throws Exception {
        TlsConfig tlsConfig = new TlsConfig();
        tlsConfig.setCertificate("cert-content");
        tlsConfig.setPrivateKey("key-content");

        CreateServerRequest request = new CreateServerRequest("tls-server", 9005, "TLS Server", tlsConfig, null, null, null);

        doNothing().when(tlsConfigService).configureTls(anyString(), any(TlsConfig.class));

        mockServerManager.createServer(request);
        verify(tlsConfigService).configureTls(eq("tls-server"), eq(tlsConfig));
    }

    @Test
    @DisplayName("Should retrieve server instance successfully")
    void testGetServerInstance() {
        CreateServerRequest request = new CreateServerRequest("test-server", 9008, "Test", null, null, null, null);
        mockServerManager.createServer(request);
        ServerInstance instance = mockServerManager.getServerInstance("test-server");

        assertNotNull(instance);
        assertEquals("test-server", instance.serverId());
    }

    @Test
    @DisplayName("Should list all servers")
    void testListServers() {
        mockServerManager.createServer(new CreateServerRequest("server1", 9009, "S1", null, null, null, null));
        mockServerManager.createServer(new CreateServerRequest("server2", 9010, "S2", null, null, null, null));

        List<ServerInfo> servers = mockServerManager.listServers();
        assertEquals(2, servers.size());
    }

    @Test
    @DisplayName("Should delete server successfully")
    void testDeleteServer() {
        mockServerManager.createServer(new CreateServerRequest("test-server", 9012, "Test", null, null, null, null));
        assertTrue(mockServerManager.serverExists("test-server"));

        boolean deleted = mockServerManager.deleteServer("test-server");

        assertTrue(deleted);
        assertFalse(mockServerManager.serverExists("test-server"));
        verify(tlsConfigService).cleanupServerCertificates("test-server");
    }

    @Test
    @DisplayName("Should return correct server count")
    void testGetServerCount() {
        assertEquals(0, mockServerManager.getServerCount());
        mockServerManager.createServer(new CreateServerRequest("server1", 9015, "S1", null, null, null, null));
        assertEquals(1, mockServerManager.getServerCount());
    }

    @Test
    @DisplayName("Should shutdown all servers on service shutdown")
    void testShutdown() {
        mockServerManager.createServer(new CreateServerRequest("server1", 9017, "S1", null, null, null, null));
        mockServerManager.shutdown();
        assertEquals(0, mockServerManager.getServerCount());
    }
}
