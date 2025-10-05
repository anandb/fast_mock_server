package io.github.anandb.mockserver.config;

import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.service.MockServerManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the application/jsonmc MIME type support.
 * <p>
 * Tests that Spring MVC correctly processes requests with Content-Type: application/jsonmc
 * by parsing JSON with comments and multiline strings, then passing it through the
 * standard JSON processing pipeline.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JsonMultilineCommentHttpMessageConverterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MockServerManager mockServerManager;

    @Test
    void testCreateServerWithJsonmcContentType() throws Exception {
        // Prepare mock response
        ServerInfo mockServerInfo = new ServerInfo();
        mockServerInfo.setServerId("test-server-1");
        mockServerInfo.setPort(8080);
        mockServerInfo.setStatus("running");

        when(mockServerManager.createServer(any(CreateServerRequest.class)))
                .thenReturn(mockServerInfo);

        // JSON with comments and multiline strings
        String jsonmcContent = """
                {
                  // This is a single-line comment
                  "serverId": "test-server-1",

                  /* This is a
                     multi-line comment */
                  "port": 8080,

                  // Using a multiline string for description
                  "description": `This is a test server
                with multiple lines
                of description text`
                }
                """;

        // Send request with application/jsonmc content type
        mockMvc.perform(post("/api/servers")
                        .contentType(new MediaType("application", "jsonmc"))
                        .content(jsonmcContent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serverId").value("test-server-1"))
                .andExpect(jsonPath("$.port").value(8080))
                .andExpect(jsonPath("$.status").value("running"));

        // Verify that the service was called with the parsed request
        verify(mockServerManager).createServer(any(CreateServerRequest.class));
    }

    @Test
    void testCreateServerWithComplexJsonmc() throws Exception {
        // Prepare mock response
        ServerInfo mockServerInfo = new ServerInfo();
        mockServerInfo.setServerId("complex-server");
        mockServerInfo.setPort(9090);
        mockServerInfo.setStatus("running");

        when(mockServerManager.createServer(any(CreateServerRequest.class)))
                .thenReturn(mockServerInfo);

        // More complex JSON with various comment styles and multiline strings
        String jsonmcContent = """
                {
                  // Server configuration
                  "serverId": "complex-server",
                  "port": 9090,

                  /* TLS Configuration
                   * Enable TLS for secure communication
                   */
                  "tlsConfig": {
                    "enabled": true,
                    // Certificate details
                    "certificate": "/path/to/cert.pem",
                    "privateKey": "/path/to/key.pem"
                  },

                  // Multiline error message template
                  "errorMessage": `Error occurred while processing request.
                Please check the following:
                1. Server is running
                2. Configuration is valid
                3. Network is accessible`
                }
                """;

        // Send request with application/jsonmc content type
        mockMvc.perform(post("/api/servers")
                        .contentType(new MediaType("application", "jsonmc"))
                        .content(jsonmcContent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serverId").value("complex-server"))
                .andExpect(jsonPath("$.port").value(9090));

        // Verify that the service was called
        verify(mockServerManager).createServer(any(CreateServerRequest.class));
    }

    @Test
    void testInvalidJsonmcReturnsError() throws Exception {
        // Invalid JSON with unclosed multiline string
        String invalidJsonmc = """
                {
                  "serverId": "test",
                  "description": `Unclosed multiline string
                }
                """;

        // Should return 400 Bad Request for invalid JSON
        mockMvc.perform(post("/api/servers")
                        .contentType(new MediaType("application", "jsonmc"))
                        .content(invalidJsonmc))
                .andExpect(status().isBadRequest());
    }
}
