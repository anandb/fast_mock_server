package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.CreateServerRequest;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test class for ConfigurationLoaderService with .jsonmc file support.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationLoaderServiceJsonmcTest {

    @Mock
    private MockServerManager mockServerManager;

    @Mock
    private ServerInstance serverInstance;

    @Mock
    private FreemarkerTemplateService freemarkerTemplateService;

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
    void testLoadConfigurationsFromJsonmcFile() throws Exception {
        String jsonmcContent = """
        [
          {
            "server": {
              "serverId": "test-server-1",
              "port": 8081,
              "description": `Multiline`
            },
            "expectations": []
          }
        ]
        """;

        Path configFile = tempDir.resolve("test-config.jsonmc");
        Files.writeString(configFile, jsonmcContent);

        when(mockServerManager.getServerInstance(anyString())).thenReturn(serverInstance);
        when(serverInstance.server()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));

        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromFile",
            configFile.toFile()
        );

        ArgumentCaptor<CreateServerRequest> requestCaptor = ArgumentCaptor.forClass(CreateServerRequest.class);
        verify(mockServerManager).createServer(requestCaptor.capture());

        CreateServerRequest capturedRequest = requestCaptor.getValue();
        assertEquals("test-server-1", capturedRequest.getServerId());
    }

    @Test
    void testJsonmcFileExtensionDetection() {
        assertTrue(isJsonmcFile("config.jsonmc"));
        assertFalse(isJsonmcFile("config.json"));
    }

    private boolean isJsonmcFile(String fileName) {
        return fileName.toLowerCase().endsWith(".jsonmc");
    }

    @Test
    void testLoadConfigurationsFromBase64() throws Exception {
        String jsonContent = "[{\"server\":{\"serverId\":\"base64-server\",\"port\":8083}}]";
        String base64Content = Base64.getEncoder().encodeToString(jsonContent.getBytes(StandardCharsets.UTF_8));

        when(mockServerManager.getServerInstance(anyString())).thenReturn(serverInstance);
        when(serverInstance.server()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));

        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromBase64",
            base64Content
        );

        verify(mockServerManager).createServer(any(CreateServerRequest.class));
    }

    private CreateServerRequest any(Class<CreateServerRequest> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
