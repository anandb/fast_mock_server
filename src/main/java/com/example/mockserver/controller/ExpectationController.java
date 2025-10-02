package com.example.mockserver.controller;

import com.example.mockserver.exception.InvalidExpectationException;
import com.example.mockserver.service.MockServerManager;
import com.example.mockserver.service.MockServerManager.ServerInstance;
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
import org.mockserver.model.HttpResponse;
import org.mockserver.mock.Expectation;
import org.springframework.http.ResponseEntity;

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
 * REST controller for configuring expectations on mock servers
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ExpectationController {

    private final MockServerManager mockServerManager;
    private final ObjectMapper objectMapper;

    /**
     * Configure expectations for a specific server
     * Merges global headers with expectation-specific headers
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

        try {
            // Parse expectations from JSON
            Expectation[] expectations = parseExpectations(expectationsJson);
            log.debug("Parsed {} expectations", expectations.length);

            // Apply global headers to each expectation and configure
            if (globalHeaders != null && !globalHeaders.isEmpty()) {
                log.debug("Applying {} global headers to expectations", globalHeaders.size());
                for (Expectation expectation : expectations) {
                    Expectation mergedExpectation = applyGlobalHeaders(expectation, globalHeaders);
                    server.when(mergedExpectation.getHttpRequest())
                          .respond(mergedExpectation.getHttpResponse());
                }
            } else {
                // No global headers, configure as-is
                for (Expectation expectation : expectations) {
                    server.when(expectation.getHttpRequest())
                          .respond(expectation.getHttpResponse());
                }
            }

            String message = String.format(
                "Successfully configured %d expectation(s) for server: %s",
                expectations.length,
                serverId
            );
            log.info(message);
            return ResponseEntity.ok(message);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse expectations JSON", e);
            throw new InvalidExpectationException(
                "Invalid expectation JSON: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Apply global headers to an expectation
     * Expectation-specific headers take precedence over global headers
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
     * Parse expectation JSON - supports single expectation or array
     */
    private Expectation[] parseExpectations(String json) throws JsonProcessingException {
        // Try parsing as array first
        try {
            return objectMapper.readValue(json, Expectation[].class);
        } catch (JsonProcessingException e) {
            // Try parsing as single expectation
            try {
                Expectation single = objectMapper.readValue(json, Expectation.class);
                return new Expectation[]{single};
            } catch (JsonProcessingException e2) {
                // Neither worked, throw original error
                throw e;
            }
        }
    }

    /**
     * Get current expectations for a server (optional feature)
     */
    @GetMapping("/{serverId}/expectations")
    public ResponseEntity<Expectation[]> getExpectations(@PathVariable String serverId) {
        log.debug("Retrieving expectations for server: {}", serverId);

        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        Expectation[] expectations = serverInstance.getServer()
            .retrieveActiveExpectations(request());

        return ResponseEntity.ok(expectations);
    }

    /**
     * Clear all expectations for a server
     */
    @DeleteMapping("/{serverId}/expectations")
    public ResponseEntity<String> clearExpectations(@PathVariable String serverId) {
        log.info("Clearing expectations for server: {}", serverId);

        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        serverInstance.getServer().reset();

        String message = String.format("Cleared all expectations for server: %s", serverId);
        log.info(message);
        return ResponseEntity.ok(message);
    }
}
