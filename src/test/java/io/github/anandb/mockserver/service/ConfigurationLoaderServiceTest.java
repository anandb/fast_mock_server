package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.EnhancedExpectation;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.model.ServerConfiguration;
import io.github.anandb.mockserver.model.ServerCreationRequest;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.strategy.ResponseStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigurationLoaderServiceTest {

    @Mock
    private MockServerManager mockServerManager;

    @Mock
    private ServerInstance serverInstance;

    private List<ResponseStrategy> strategies = new ArrayList<>();
    private ConfigurationLoaderService configurationLoaderService;
    private ObjectMapper objectMapper;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        configurationLoaderService = new ConfigurationLoaderService(mockServerManager, objectMapper, strategies);
    }

    @Test
    void testJsoncFileExtensionDetection() throws Exception {
        String jsoncContent = """
        [
          {
            "server": {
              "serverId": "test-server-jsonc",
              "port": 8082
            }
          }
        ]
        """;

        Path configFile = tempDir.resolve("test-config.jsonc");
        Files.writeString(configFile, jsoncContent);

        when(mockServerManager.getServerInstance(anyString())).thenReturn(serverInstance);
        when(serverInstance.server()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));

        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromFile",
            configFile.toFile()
        );

        ArgumentCaptor<ServerCreationRequest> requestCaptor = ArgumentCaptor.forClass(ServerCreationRequest.class);
        verify(mockServerManager).createServer(requestCaptor.capture());

        ServerCreationRequest capturedRequest = requestCaptor.getValue();
        assertEquals("test-server-jsonc", capturedRequest.getServerId());
    }

    @Test
    void testServerWithRelays() throws Exception {
        String jsonContent = """
        [
          {
            "server": {
              "serverId": "test-server-with-relays",
              "port": 8084
            }
          }
        ]
        """;

        Path configFile = tempDir.resolve("test-config-relays.json");
        Files.writeString(configFile, jsonContent);

        RelayConfig relayConfig = mock(RelayConfig.class);
        when(relayConfig.isValid()).thenReturn(true);

        when(mockServerManager.getServerInstance(anyString())).thenReturn(serverInstance);
        when(serverInstance.server()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));
        when(serverInstance.serverId()).thenReturn("test-server-with-relays");
        when(serverInstance.relays()).thenReturn(List.of(relayConfig));
        when(serverInstance.globalHeaders()).thenReturn(List.of());

        assertThrows(Exception.class, () -> {
            ReflectionTestUtils.invokeMethod(
                configurationLoaderService,
                "loadConfigurationsFromFile",
                configFile.toFile()
            );
        });

        verify(mockServerManager, org.mockito.Mockito.atLeast(1)).createServer(any(ServerCreationRequest.class));
    }

    @Test
    void testInvalidJsonContent() throws Exception {
        String invalidJsonContent = """
        [
          {
            "server": {
              "serverId": "test-server"
              "port": 8085
            }
          }
        ]
        """;

        Path configFile = tempDir.resolve("invalid-config.json");
        Files.writeString(configFile, invalidJsonContent);

        assertThrows(Exception.class, () -> {
            ReflectionTestUtils.invokeMethod(
                configurationLoaderService,
                "loadConfigurationsFromFile",
                configFile.toFile()
            );
        });
    }

    @Test
    void testEmptyExpectationsList() throws Exception {
        String jsonContent = """
        [
          {
            "server": {
              "serverId": "test-server-empty-expectations",
              "port": 8086
            },
            "expectations": []
          }
        ]
        """;

        Path configFile = tempDir.resolve("test-config-empty.json");
        Files.writeString(configFile, jsonContent);

        when(mockServerManager.getServerInstance(anyString())).thenReturn(serverInstance);
        when(serverInstance.server()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));
        when(serverInstance.serverId()).thenReturn("test-server-empty-expectations");
        when(serverInstance.relays()).thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromFile",
            configFile.toFile()
        );

        verify(mockServerManager, org.mockito.Mockito.atLeast(1)).createServer(any(ServerCreationRequest.class));
    }

    @Test
    void testServerConfigurationHasExpectations() {
        ServerConfiguration configWithExpectations = new ServerConfiguration(
            mock(ServerCreationRequest.class),
            List.of(mock(EnhancedExpectation.class))
        );
        assertTrue(configWithExpectations.hasExpectations());

        ServerConfiguration configWithEmptyExpectations = new ServerConfiguration(
            mock(ServerCreationRequest.class),
            List.of()
        );
        assertFalse(configWithEmptyExpectations.hasExpectations());

        ServerConfiguration configWithNullExpectations = new ServerConfiguration(
            mock(ServerCreationRequest.class),
            null
        );
        assertFalse(configWithNullExpectations.hasExpectations());
    }

    @Test
    void testMultipleServerConfigurations() throws Exception {
        String jsonContent = """
        [
          {
            "server": {
              "serverId": "server-1",
              "port": 9001
            }
          },
          {
            "server": {
              "serverId": "server-2",
              "port": 9002
            }
          }
        ]
        """;

        Path configFile = tempDir.resolve("multi-config.json");
        Files.writeString(configFile, jsonContent);

        when(mockServerManager.getServerInstance("server-1")).thenReturn(serverInstance);
        when(mockServerManager.getServerInstance("server-2")).thenReturn(serverInstance);
        when(serverInstance.server()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));
        when(serverInstance.relays()).thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromFile",
            configFile.toFile()
        );

        verify(mockServerManager, org.mockito.Mockito.atLeast(1)).createServer(any(ServerCreationRequest.class));
    }

    @Test
    void testFileNotFoundHandling() {
        Path nonExistentFile = tempDir.resolve("non-existent.json");

        assertThrows(Exception.class, () -> {
            ReflectionTestUtils.invokeMethod(
                configurationLoaderService,
                "loadConfigurationsFromFile",
                nonExistentFile.toFile()
            );
        });
    }
}
