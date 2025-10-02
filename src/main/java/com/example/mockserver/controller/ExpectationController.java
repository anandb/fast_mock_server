package com.example.mockserver.controller;

import com.example.mockserver.exception.InvalidExpectationException;
import com.example.mockserver.service.MockServerManager;
import com.example.mockserver.service.MockServerManager.ServerInstance;
import com.example.mockserver.service.MockServerOperations;
import com.example.mockserver.service.MockServerOperationsImpl;
import com.example.mockserver.model.GlobalHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.mock.Expectation;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.stream.Collectors;

import org.mockserver.model.NottableString;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;

/**
 * REST controller for configuring expectations on mock servers.
 * <p>
 * Provides endpoints to add, retrieve, and clear expectations for mock server instances.
 * Supports merging global headers with expectation-specific headers.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ExpectationController {

    private final MockServerManager mockServerManager;
    private final ObjectMapper objectMapper;

    /**
     * Configures expectations for a specific mock server.
     * <p>
     * This method accepts expectation JSON (single or array) and merges any global headers
     * configured for the server with the expectation-specific headers. Expectation-specific
     * headers take precedence over global headers in case of conflicts.
     * </p>
     *
     * @param serverId the unique identifier of the server
     * @param expectationsJson JSON string containing one or more expectations
     * @return ResponseEntity with a success message
     * @throws ServerNotFoundException if the server with the specified ID is not found
     * @throws InvalidExpectationException if the expectation JSON is invalid
     */
    @PostMapping("/{serverId}/expectations")
    public ResponseEntity<String> configureExpectations(
        @PathVariable String serverId,
        @RequestBody String expectationsJson
    ) {
        log.info("Configuring expectations for server: {}", serverId);

        // Get server instance
        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        ClientAndServer server = serverInstance.getServer();
        List<GlobalHeader> globalHeaders = serverInstance.getGlobalHeaders();

        // Create MockServerOperations instance for this server
        MockServerOperations mockServerOperations = new MockServerOperationsImpl(server);

        // Parse expectations from JSON
        Expectation[] expectations = parseExpectations(expectationsJson);
        log.debug("Parsed {} expectations", expectations.length);

        // Apply global headers and basic auth to each expectation and configure
        for (Expectation expectation : expectations) {
            // Apply global headers if present
            Expectation processedExpectation = expectation;
            if (globalHeaders != null && !globalHeaders.isEmpty()) {
                log.debug("Applying {} global headers to expectation", globalHeaders.size());
                processedExpectation = applyGlobalHeaders(processedExpectation, globalHeaders);
            }

            // Apply basic auth requirement if enabled
            if (serverInstance.isBasicAuthEnabled()) {
                log.debug("Applying basic auth requirement to expectation");
                processedExpectation = applyBasicAuthRequirement(
                    processedExpectation,
                    serverInstance.getBasicAuthConfig()
                );
            }

            mockServerOperations.configureExpectation(
                processedExpectation.getHttpRequest(),
                processedExpectation.getHttpResponse()
            );
        }

        String message = String.format(
            "Successfully configured %d expectation(s) for server: %s",
            expectations.length,
            serverId
        );
        log.info(message);
        return ResponseEntity.ok(message);
    }

    /**
     * Applies basic authentication requirement to an expectation's HTTP request.
     * <p>
     * Adds an Authorization header matcher to the request that validates the provided
     * credentials match the server's configured basic auth credentials.
     * </p>
     *
     * @param expectation the original expectation to enhance
     * @param basicAuthConfig the basic auth configuration with username and password
     * @return a new Expectation with basic auth requirement added
     */
    private Expectation applyBasicAuthRequirement(
        Expectation expectation,
        com.example.mockserver.model.BasicAuthConfig basicAuthConfig
    ) {
        // Generate expected Authorization header value
        String credentials = basicAuthConfig.getUsername() + ":" + basicAuthConfig.getPassword();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        String expectedAuthHeader = "Basic " + encodedCredentials;

        // Get original request and add Authorization header requirement
        // Cast RequestDefinition to HttpRequest (which is what we expect from expectations)
        HttpRequest requestWithAuth = ((HttpRequest) expectation.getHttpRequest())
            .withHeader("Authorization", expectedAuthHeader);

        // Return new expectation with modified request
        return new Expectation(requestWithAuth)
            .thenRespond(expectation.getHttpResponse());
    }

    /**
     * Applies global headers to an expectation's HTTP response.
     * <p>
     * Merges global headers with expectation-specific headers. If a header with the same
     * name exists in both, the expectation-specific header takes precedence.
     * </p>
     *
     * @param expectation the original expectation to enhance
     * @param globalHeaders list of global headers to apply
     * @return a new Expectation with merged headers
     */
    private Expectation applyGlobalHeaders(
        Expectation expectation,
        List<GlobalHeader> globalHeaders
    ) {
        HttpResponse originalResponse = expectation.getHttpResponse();

        // Get existing headers from expectation
        List<Header> existingHeaders = originalResponse.getHeaderList() != null
            ? new ArrayList<>(originalResponse.getHeaderList())
            : new ArrayList<>();

        // Convert existing headers to map for easy lookup
        Map<NottableString, Header> headerMap = existingHeaders.stream().collect(Collectors.toMap(
                Header::getName,
                h -> h,
                (h1, h2) -> h1  // Keep first if duplicate
            ));

        // Add global headers (only if not already present in expectation)
        for (GlobalHeader globalHeader : globalHeaders) {
            NottableString headerName = NottableString.string(globalHeader.getName());
            if (!headerMap.containsKey(headerName)) {
                // Global header not in expectation, add it
                headerMap.put(
                    headerName,
                    header(globalHeader.getName(), globalHeader.getValue())
                );
                log.trace("Added global header: {} = {}", globalHeader.getName(), globalHeader.getValue());
            } else {
                // Header exists in expectation, it takes precedence
                log.trace("Expectation header overrides global: {}", globalHeader.getName());
            }
        }

        // Create new header array from merged map
        Header[] mergedHeaders = headerMap.values().toArray(new Header[0]);

        // Create new response with merged headers
        HttpResponse mergedResponse = originalResponse.withHeaders(mergedHeaders);

        // Return new expectation with merged response
        return new Expectation(expectation.getHttpRequest())
            .thenRespond(mergedResponse);
    }

    /**
     * Parses expectation JSON into an array of Expectation objects.
     * <p>
     * Supports both single expectation objects and arrays of expectations.
     * Uses MockServer's serialization for proper parsing.
     * </p>
     *
     * @param json JSON string to parse
     * @return array of Expectation objects
     * @throws InvalidExpectationException if the JSON cannot be parsed
     */
    private Expectation[] parseExpectations(String json) {
        json = json.trim();

        org.mockserver.serialization.ExpectationSerializer serializer =
            new org.mockserver.serialization.ExpectationSerializer(new org.mockserver.logging.MockServerLogger());

        // Check if it's an array
        if (json.startsWith("[")) {
            // Parse as array using MockServer's serializer
            try {
                return serializer.deserializeArray(json, false);
            } catch (Exception e) {
                log.error("Failed to parse expectations array", e);
                throw new InvalidExpectationException("Failed to parse expectations array: " + e.getMessage(), e);
            }
        } else {
            // Parse as single expectation using MockServer's serializer
            try {
                Expectation single = serializer.deserialize(json);
                return new Expectation[]{single};
            } catch (Exception e) {
                log.error("Failed to parse expectation", e);
                throw new InvalidExpectationException("Failed to parse expectation: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Retrieves all active expectations for a specific mock server.
     *
     * @param serverId the unique identifier of the server
     * @return ResponseEntity containing an array of active expectations
     * @throws ServerNotFoundException if the server with the specified ID is not found
     */
    @GetMapping("/{serverId}/expectations")
    public ResponseEntity<Expectation[]> getExpectations(@PathVariable String serverId) {
        log.debug("Retrieving expectations for server: {}", serverId);

        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        ClientAndServer server = serverInstance.getServer();
        MockServerOperations mockServerOperations = new MockServerOperationsImpl(server);

        Expectation[] expectations = mockServerOperations
            .retrieveActiveExpectations(request());

        return ResponseEntity.ok(expectations);
    }

    /**
     * Clears all expectations for a specific mock server.
     * <p>
     * This resets the server to its initial state with no configured expectations.
     * </p>
     *
     * @param serverId the unique identifier of the server
     * @return ResponseEntity with a success message
     * @throws ServerNotFoundException if the server with the specified ID is not found
     */
    @DeleteMapping("/{serverId}/expectations")
    public ResponseEntity<String> clearExpectations(@PathVariable String serverId) {
        log.info("Clearing expectations for server: {}", serverId);

        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        ClientAndServer server = serverInstance.getServer();
        MockServerOperations mockServerOperations = new MockServerOperationsImpl(server);

        mockServerOperations.reset();

        String message = String.format("Cleared all expectations for server: %s", serverId);
        log.info(message);
        return ResponseEntity.ok(message);
    }
}
