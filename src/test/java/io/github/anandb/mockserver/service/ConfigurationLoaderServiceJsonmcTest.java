package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.CreateServerRequest;
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
 * Verifies that .jsonmc files are properly processed through JsonCommentParser.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationLoaderServiceJsonmcTest {

    @Mock
    private MockServerManager mockServerManager;

    @Mock
    private MockServerManager.ServerInstance serverInstance;

    private ConfigurationLoaderService configurationLoaderService;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        configurationLoaderService = new ConfigurationLoaderService(mockServerManager, objectMapper);
    }

    @Test
    void testLoadConfigurationsFromJsonmcFile() throws Exception {
        // Create a .jsonmc file with comments and multiline strings
        String jsonmcContent = """
        [
          // This is a test server configuration with comments
          {
            "server": {
              "serverId": "test-server-1",
              "port": 8081,
              /* Multi-line comment
                 explaining the configuration */
              "description": \"\"\"
This is a multiline string
that spans multiple lines
and should be properly parsed
\"\"\"
            },
            "expectations": [
              {
                "httpRequest": {
                  "path": "/test"
                },
                "httpResponse": {
                  "statusCode": 200,
                  "body": "Test response"
                }
              }
            ]
          }
        ]
        """;

        // Write the content to a .jsonmc file
        Path configFile = tempDir.resolve("test-config.jsonmc");
        Files.writeString(configFile, jsonmcContent);

        // Mock the server instance
        when(mockServerManager.getServerInstance(anyString())).thenReturn(serverInstance);
        when(serverInstance.getServer()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));
        when(serverInstance.getGlobalHeaders()).thenReturn(null);

        // Call the private loadConfigurations method using reflection
        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurations",
            configFile.toFile()
        );

        // Verify that createServer was called
        ArgumentCaptor<CreateServerRequest> requestCaptor = ArgumentCaptor.forClass(CreateServerRequest.class);
        verify(mockServerManager).createServer(requestCaptor.capture());

        // Verify the server configuration was parsed correctly
        CreateServerRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals("test-server-1", capturedRequest.getServerId());
        assertEquals(8081, capturedRequest.getPort());

        // Verify the multiline description was properly processed
        assertNotNull(capturedRequest.getDescription());
        assertTrue(capturedRequest.getDescription().contains("multiline string"));
        assertTrue(capturedRequest.getDescription().contains("spans multiple lines"));
    }

    @Test
    void testLoadConfigurationsFromRegularJsonFile() throws Exception {
        // Create a regular .json file (without comments)
        String jsonContent = """
        [
          {
            "server": {
              "serverId": "test-server-2",
              "port": 8082,
              "description": "Regular JSON file"
            },
            "expectations": []
          }
        ]
        """;

        // Write the content to a .json file
        Path configFile = tempDir.resolve("test-config.json");
        Files.writeString(configFile, jsonContent);

        // Call the private loadConfigurations method using reflection
        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurations",
            configFile.toFile()
        );

        // Verify that createServer was called
        ArgumentCaptor<CreateServerRequest> requestCaptor = ArgumentCaptor.forClass(CreateServerRequest.class);
        verify(mockServerManager).createServer(requestCaptor.capture());

        // Verify the server configuration was parsed correctly
        CreateServerRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals("test-server-2", capturedRequest.getServerId());
        assertEquals(8082, capturedRequest.getPort());
        assertEquals("Regular JSON file", capturedRequest.getDescription());
    }

    @Test
    void testJsonmcFileExtensionDetection() {
        // Test various file extensions
        assertTrue(isJsonmcFile("config.jsonmc"));
        assertTrue(isJsonmcFile("CONFIG.JSONMC"));
        assertTrue(isJsonmcFile("test.JsonMc"));
        assertFalse(isJsonmcFile("config.json"));
        assertFalse(isJsonmcFile("config.txt"));
        assertFalse(isJsonmcFile("jsonmc"));
    }

    private boolean isJsonmcFile(String fileName) {
        return fileName.toLowerCase().endsWith(".jsonmc");
    }
}
