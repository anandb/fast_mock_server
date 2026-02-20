package io.github.anandb.mockserver.service;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfigurationLoaderServiceVariableExpansionTest {

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
    void testVariableExpansionInConfig() throws Exception {
        // We use a variable that is almost certainly present in the environment
        // if not, we use the default value.
        String jsonmcContent = """
        [
          {
            "server": {
              "serverId": "@{TEST_SERVER_ID:-expanded-server}",
              "port": 8081
            }
          }
        ]
        """;

        Path configFile = tempDir.resolve("test-expand.jsonmc");
        Files.writeString(configFile, jsonmcContent);

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
        // Since TEST_SERVER_ID is likely not set, it should expand to the default value
        assertEquals("expanded-server", capturedRequest.getServerId());
    }
}
