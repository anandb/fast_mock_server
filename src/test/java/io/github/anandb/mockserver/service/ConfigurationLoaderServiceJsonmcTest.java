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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

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

    @Mock
    private FreemarkerTemplateService freemarkerTemplateService;

    private ConfigurationLoaderService configurationLoaderService;
    private ObjectMapper objectMapper;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        configurationLoaderService = new ConfigurationLoaderService(mockServerManager, objectMapper, freemarkerTemplateService);
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
              "description": `
This is a multiline string
that spans multiple lines
and should be properly parsed
`
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

        // Call the private loadConfigurationsFromFile method using reflection
        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromFile",
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

        // Call the private loadConfigurationsFromFile method using reflection
        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromFile",
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

    @Test
    void testLoadConfigurationsFromBase64() throws Exception {
        // Create a JSON configuration string
        String jsonContent = """
        [
          {
            "server": {
              "serverId": "base64-server",
              "port": 8083,
              "description": "Server loaded from base64"
            },
            "expectations": [
              {
                "httpRequest": {
                  "path": "/api/test"
                },
                "httpResponse": {
                  "statusCode": 200,
                  "body": "{\\"message\\": \\"Hello from base64\\"}"
                }
              }
            ]
          }
        ]
        """;

        // Encode the content to base64
        String base64Content = Base64.getEncoder().encodeToString(jsonContent.getBytes(StandardCharsets.UTF_8));

        // Mock the server instance
        when(mockServerManager.getServerInstance(anyString())).thenReturn(serverInstance);
        when(serverInstance.getServer()).thenReturn(mock(org.mockserver.integration.ClientAndServer.class));
        when(serverInstance.getGlobalHeaders()).thenReturn(null);

        // Call the private loadConfigurationsFromBase64 method using reflection
        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromBase64",
            base64Content
        );

        // Verify that createServer was called
        ArgumentCaptor<CreateServerRequest> requestCaptor = ArgumentCaptor.forClass(CreateServerRequest.class);
        verify(mockServerManager).createServer(requestCaptor.capture());

        // Verify the server configuration was parsed correctly
        CreateServerRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals("base64-server", capturedRequest.getServerId());
        assertEquals(8083, capturedRequest.getPort());
        assertEquals("Server loaded from base64", capturedRequest.getDescription());
    }

    @Test
    void testLoadConfigurationsFromBase64WithJsonmc() throws Exception {
        // Create a JSONMC configuration string with comments
        String jsonmcContent = """
        [
          // Base64-encoded JSONMC configuration
          {
            "server": {
              "serverId": "base64-jsonmc-server",
              "port": 8084,
              /* This is a multiline comment
                 in a base64-encoded config */
              "description": `
Multiline description
from base64-encoded JSONMC
`
            }
          }
        ]
        """;

        // Encode the content to base64
        String base64Content = Base64.getEncoder().encodeToString(jsonmcContent.getBytes(StandardCharsets.UTF_8));

        // Call the private loadConfigurationsFromBase64 method using reflection
        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromBase64",
            base64Content
        );

        // Verify that createServer was called
        ArgumentCaptor<CreateServerRequest> requestCaptor = ArgumentCaptor.forClass(CreateServerRequest.class);
        verify(mockServerManager).createServer(requestCaptor.capture());

        // Verify the server configuration was parsed correctly
        CreateServerRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals("base64-jsonmc-server", capturedRequest.getServerId());
        assertEquals(8084, capturedRequest.getPort());

        // Verify the multiline description was properly processed
        assertNotNull(capturedRequest.getDescription());
        assertTrue(capturedRequest.getDescription().contains("Multiline description"));
        assertTrue(capturedRequest.getDescription().contains("base64-encoded JSONMC"));
    }

    @Test
    void testBase64AutoDetectionOfJsonmc() throws Exception {
        // Create content with comment markers that should be auto-detected as JSONMC
        String contentWithComments = """
        // This should be detected as JSONMC
        [
          {
            "server": {
              "serverId": "auto-detect-server",
              "port": 8085
            }
          }
        ]
        """;

        // Encode to base64
        String base64Content = Base64.getEncoder().encodeToString(
            contentWithComments.getBytes(StandardCharsets.UTF_8)
        );

        // Call the method
        ReflectionTestUtils.invokeMethod(
            configurationLoaderService,
            "loadConfigurationsFromBase64",
            base64Content
        );

        // Verify that createServer was called, confirming the content was parsed
        ArgumentCaptor<CreateServerRequest> requestCaptor = ArgumentCaptor.forClass(CreateServerRequest.class);
        verify(mockServerManager).createServer(requestCaptor.capture());

        // Verify successful parsing
        CreateServerRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        assertEquals("auto-detect-server", capturedRequest.getServerId());
        assertEquals(8085, capturedRequest.getPort());
    }
}
