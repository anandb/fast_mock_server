package io.github.anandb.mockserver.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.ServerInfo;

/**
 * Integration tests for the MockServer application.
 * <p>
 * Tests the entire application stack including REST endpoints, services,
 * and MockServer lifecycle management in a real Spring Boot context.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("MockServer Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MockServerIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        private String baseUrl;

        @BeforeEach
        void setUp() {
                baseUrl = "http://localhost:" + port + "/api/servers";
        }

        // End-to-End Server Lifecycle Tests

        @Test
        @Order(1)
        @DisplayName("Should create, retrieve, and delete HTTP server successfully")
        void testHttpServerLifecycle() {
                // Create server
                CreateServerRequest request = new CreateServerRequest(
                                "integration-test-server",
                                9100,
                                "Integration Test Server",
                                null,
                                null,
                                null,
                                null);

                ResponseEntity<ServerInfo> createResponse = restTemplate.postForEntity(
                                baseUrl,
                                request,
                                ServerInfo.class);

                assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
                assertNotNull(createResponse.getBody());
                assertEquals("integration-test-server", createResponse.getBody().getServerId());
                assertEquals(9100, createResponse.getBody().getPort());
                assertEquals("http", createResponse.getBody().getProtocol());
                assertEquals("running", createResponse.getBody().getStatus());

                // Retrieve server
                ResponseEntity<ServerInfo> getResponse = restTemplate.getForEntity(
                                baseUrl + "/integration-test-server",
                                ServerInfo.class);

                assertEquals(HttpStatus.OK, getResponse.getStatusCode());
                assertNotNull(getResponse.getBody());
                assertEquals("integration-test-server", getResponse.getBody().getServerId());

                // Delete server
                restTemplate.delete(baseUrl + "/integration-test-server");

                // Verify deletion
                ResponseEntity<String> verifyResponse = restTemplate.getForEntity(
                                baseUrl + "/integration-test-server",
                                String.class);

                assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
        }

        @Test
        @Order(2)
        @DisplayName("Should create server with global headers and configure expectations")
        void testServerWithGlobalHeadersAndExpectations() {
                // Create server with global headers
                List<GlobalHeader> globalHeaders = new ArrayList<>();
                globalHeaders.add(new GlobalHeader("X-Service-Name", "test-service"));
                globalHeaders.add(new GlobalHeader("X-Version", "1.0"));

                CreateServerRequest request = new CreateServerRequest(
                                "headers-test-server",
                                9101,
                                "Server with Headers",
                                null,
                                globalHeaders,
                                null,
                                null);

                ResponseEntity<ServerInfo> createResponse = restTemplate.postForEntity(
                                baseUrl,
                                request,
                                ServerInfo.class);

                assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
                assertNotNull(createResponse.getBody());
                assertEquals(2, createResponse.getBody().getGlobalHeaders().size());

                // Configure expectation
                String expectationJson = """
                                {
                                    "httpRequest": {
                                        "method": "GET",
                                        "path": "/test"
                                    },
                                    "httpResponse": {
                                        "statusCode": 200,
                                        "body": {"message": "Hello"}
                                    }
                                }
                                """;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> expectationRequest = new HttpEntity<>(expectationJson, headers);

                ResponseEntity<String> expectationResponse = restTemplate.postForEntity(
                                baseUrl + "/headers-test-server/expectations",
                                expectationRequest,
                                String.class);

                assertEquals(HttpStatus.OK, expectationResponse.getStatusCode());
                assertTrue(expectationResponse.getBody().contains("Successfully configured"));

                // Cleanup
                restTemplate.delete(baseUrl + "/headers-test-server");
        }

        @Test
        @Order(3)
        @DisplayName("Should list multiple servers")
        void testListMultipleServers() {
                // Create multiple servers
                CreateServerRequest request1 = new CreateServerRequest("server1", 9102, "Server 1", null, null, null, null);
                CreateServerRequest request2 = new CreateServerRequest("server2", 9103, "Server 2", null, null, null, null);
                CreateServerRequest request3 = new CreateServerRequest("server3", 9104, "Server 3", null, null, null, null);

                restTemplate.postForEntity(baseUrl, request1, ServerInfo.class);
                restTemplate.postForEntity(baseUrl, request2, ServerInfo.class);
                restTemplate.postForEntity(baseUrl, request3, ServerInfo.class);

                // List all servers
                ResponseEntity<ServerInfo[]> listResponse = restTemplate.getForEntity(
                                baseUrl,
                                ServerInfo[].class);

                assertEquals(HttpStatus.OK, listResponse.getStatusCode());
                assertNotNull(listResponse.getBody());
                assertEquals(3, listResponse.getBody().length);

                // Cleanup
                restTemplate.delete(baseUrl + "/server1");
                restTemplate.delete(baseUrl + "/server2");
                restTemplate.delete(baseUrl + "/server3");
        }

        @Test
        @Order(4)
        @DisplayName("Should handle duplicate server creation error")
        void testDuplicateServerError() {
                // Create first server
                CreateServerRequest request = new CreateServerRequest(
                                "duplicate-test",
                                9105,
                                "Original Server",
                                null,
                                null,
                                null,
                                null);

                ResponseEntity<ServerInfo> firstResponse = restTemplate.postForEntity(
                                baseUrl,
                                request,
                                ServerInfo.class);

                assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());

                // Try to create duplicate
                ResponseEntity<String> duplicateResponse = restTemplate.postForEntity(
                                baseUrl,
                                request,
                                String.class);

                assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
                assertTrue(duplicateResponse.getBody().contains("SERVER_ALREADY_EXISTS"));

                // Cleanup
                restTemplate.delete(baseUrl + "/duplicate-test");
        }

        @Test
        @Order(5)
        @DisplayName("Should validate port range")
        void testPortValidation() {
                // Test invalid port (too low)
                CreateServerRequest invalidRequest1 = new CreateServerRequest(
                                "invalid-port-1",
                                100,
                                "Invalid Port",
                                null,
                                null,
                                null,
                                null);

                ResponseEntity<String> response1 = restTemplate.postForEntity(
                                baseUrl,
                                invalidRequest1,
                                String.class);

                assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());

                // Test invalid port (too high)
                CreateServerRequest invalidRequest2 = new CreateServerRequest(
                                "invalid-port-2",
                                70000,
                                "Invalid Port",
                                null,
                                null,
                                null,
                                null);

                ResponseEntity<String> response2 = restTemplate.postForEntity(
                                baseUrl,
                                invalidRequest2,
                                String.class);

                assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        }

        @Test
        @Order(6)
        @DisplayName("Should check server existence")
        void testServerExistence() {
                // Create a server
                CreateServerRequest request = new CreateServerRequest(
                                "exists-test",
                                9106,
                                "Existence Test",
                                null,
                                null,
                                null,
                                null);

                restTemplate.postForEntity(baseUrl, request, ServerInfo.class);

                // Check existence (should exist)
                ResponseEntity<Boolean> existsResponse = restTemplate.getForEntity(
                                baseUrl + "/exists-test/exists",
                                Boolean.class);

                assertEquals(HttpStatus.OK, existsResponse.getStatusCode());
                assertTrue(existsResponse.getBody());

                // Check non-existent server
                ResponseEntity<Boolean> notExistsResponse = restTemplate.getForEntity(
                                baseUrl + "/non-existent/exists",
                                Boolean.class);

                assertEquals(HttpStatus.OK, notExistsResponse.getStatusCode());
                assertFalse(notExistsResponse.getBody());

                // Cleanup
                restTemplate.delete(baseUrl + "/exists-test");
        }

        @Test
        @Order(7)
        @DisplayName("Should clear expectations successfully")
        void testClearExpectations() {
                // Create server
                CreateServerRequest request = new CreateServerRequest(
                                "clear-test",
                                9107,
                                "Clear Test",
                                null,
                                null,
                                null,
                                null);

                restTemplate.postForEntity(baseUrl, request, ServerInfo.class);

                // Configure expectation
                String expectationJson = """
                                {
                                    "httpRequest": {
                                        "method": "GET",
                                        "path": "/data"
                                    },
                                    "httpResponse": {
                                        "statusCode": 200,
                                        "body": {}
                                    }
                                }
                                """;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> expectationRequest = new HttpEntity<>(expectationJson, headers);

                restTemplate.postForEntity(
                                baseUrl + "/clear-test/expectations",
                                expectationRequest,
                                String.class);

                // Clear expectations
                restTemplate.delete(baseUrl + "/clear-test/expectations");

                // Retrieve expectations (should be empty)
                ResponseEntity<String> retrieveResponse = restTemplate.getForEntity(
                                baseUrl + "/clear-test/expectations",
                                String.class);

                assertEquals(HttpStatus.OK, retrieveResponse.getStatusCode());

                // Cleanup
                restTemplate.delete(baseUrl + "/clear-test");
        }

        @Test
        @Order(8)
        @DisplayName("Should handle server not found errors")
        void testServerNotFoundErrors() {
                // Try to get non-existent server
                ResponseEntity<String> getResponse = restTemplate.getForEntity(
                                baseUrl + "/non-existent-server",
                                String.class);

                assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
                assertTrue(getResponse.getBody().contains("SERVER_NOT_FOUND"));

                // Try to delete non-existent server
                restTemplate.delete(baseUrl + "/non-existent-server");

                // Try to configure expectations on non-existent server
                String expectationJson = """
                                {
                                    "httpRequest": {"method": "GET", "path": "/test"},
                                    "httpResponse": {"statusCode": 200}
                                }
                                """;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> request = new HttpEntity<>(expectationJson, headers);

                ResponseEntity<String> expectationResponse = restTemplate.postForEntity(
                                baseUrl + "/non-existent-server/expectations",
                                request,
                                String.class);

                assertEquals(HttpStatus.NOT_FOUND, expectationResponse.getStatusCode());
        }

        @Test
        @Order(9)
        @DisplayName("Should validate request body fields")
        void testRequestValidation() {
                // Missing serverId
                CreateServerRequest invalidRequest1 = new CreateServerRequest(
                                null,
                                9108,
                                "Missing ID",
                                null,
                                null,
                                null,
                                null);

                ResponseEntity<String> response1 = restTemplate.postForEntity(
                                baseUrl,
                                invalidRequest1,
                                String.class);

                assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());

                // Missing port
                CreateServerRequest invalidRequest2 = new CreateServerRequest(
                                "missing-port",
                                null,
                                "Missing Port",
                                null,
                                null,
                                null,
                                null);

                ResponseEntity<String> response2 = restTemplate.postForEntity(
                                baseUrl,
                                invalidRequest2,
                                String.class);

                assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        }

        @Test
        @Order(10)
        @DisplayName("Should configure multiple expectations")
        void testMultipleExpectations() {
                // Create server
                CreateServerRequest request = new CreateServerRequest(
                                "multi-exp-test",
                                9109,
                                "Multiple Expectations Test",
                                null,
                                null,
                                null,
                                null);

                restTemplate.postForEntity(baseUrl, request, ServerInfo.class);

                // Configure multiple expectations
                String expectationsJson = """
                                [
                                    {
                                        "httpRequest": {"method": "GET", "path": "/users"},
                                        "httpResponse": {"statusCode": 200, "body": {"users": []}}
                                    },
                                    {
                                        "httpRequest": {"method": "POST", "path": "/users"},
                                        "httpResponse": {"statusCode": 201, "body": {"id": 1}}
                                    },
                                    {
                                        "httpRequest": {"method": "DELETE", "path": "/users/1"},
                                        "httpResponse": {"statusCode": 204}
                                    }
                                ]
                                """;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> expectationRequest = new HttpEntity<>(expectationsJson, headers);

                ResponseEntity<String> expectationResponse = restTemplate.postForEntity(
                                baseUrl + "/multi-exp-test/expectations",
                                expectationRequest,
                                String.class);

                assertEquals(HttpStatus.OK, expectationResponse.getStatusCode());
                assertTrue(expectationResponse.getBody().contains("3 expectation"));

                // Cleanup
                restTemplate.delete(baseUrl + "/multi-exp-test");
        }
}
