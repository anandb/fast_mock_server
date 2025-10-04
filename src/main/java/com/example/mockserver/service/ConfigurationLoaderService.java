package com.example.mockserver.service;

import com.example.mockserver.exception.ServerCreationException;
import com.example.mockserver.model.CreateServerRequest;
import com.example.mockserver.model.ServerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service responsible for loading server and expectation configurations from a JSON file.
 * <p>
 * The JSON file path can be specified using the system property 'mock.server.config.file'.
 * If the property is not set or the file doesn't exist, the service will log a message
 * and continue without loading any configurations.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationLoaderService {

    private final MockServerManager mockServerManager;
    private final ObjectMapper objectMapper;

    /** System property name for specifying the configuration file path */
    private static final String CONFIG_FILE_PROPERTY = "mock.server.config.file";

    /**
     * Loads server configurations from JSON file on application startup.
     * <p>
     * This method is automatically called after the Spring context is initialized.
     * It reads the file path from the system property, validates the file exists,
     * and then loads all server configurations and their expectations.
     * </p>
     */
    @PostConstruct
    public void loadConfigurationsOnStartup() {
        String configFilePath = System.getProperty(CONFIG_FILE_PROPERTY);

        if (configFilePath == null || configFilePath.trim().isEmpty()) {
            log.info("No configuration file specified. Use -D{}=<path> to load servers from a JSON file.",
                CONFIG_FILE_PROPERTY);
            return;
        }

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            log.warn("Configuration file not found: {}", configFilePath);
            return;
        }

        if (!configFile.isFile()) {
            log.warn("Configuration path is not a file: {}", configFilePath);
            return;
        }

        log.info("Loading server configurations from: {}", configFilePath);

        try {
            loadConfigurations(configFile);
        } catch (Exception e) {
            log.error("Failed to load configurations from file: {}", configFilePath, e);
            throw new ServerCreationException(
                "Failed to load configurations from file: " + configFilePath, e);
        }
    }

    /**
     * Loads and processes all server configurations from the specified file.
     *
     * @param configFile the JSON configuration file to read
     * @throws IOException if the file cannot be read or parsed
     */
    private void loadConfigurations(File configFile) throws IOException {
        // Parse the JSON file into ServerConfiguration array
        ServerConfiguration[] configurations = objectMapper.readValue(
            configFile,
            ServerConfiguration[].class
        );

        if (configurations == null || configurations.length == 0) {
            log.warn("No server configurations found in file: {}", configFile.getAbsolutePath());
            return;
        }

        log.info("Found {} server configuration(s) to load", configurations.length);

        // Process each server configuration
        for (ServerConfiguration config : configurations) {
            try {
                processServerConfiguration(config);
            } catch (Exception e) {
                log.error("Failed to process configuration for server: {}",
                    config.getServer().getServerId(), e);
                // Continue processing other configurations
            }
        }

        log.info("Successfully loaded {} server configuration(s)", configurations.length);
    }

    /**
     * Processes a single server configuration by creating the server
     * and configuring its expectations.
     *
     * @param config the server configuration to process
     */
    private void processServerConfiguration(ServerConfiguration config) {
        CreateServerRequest serverRequest = config.getServer();
        String serverId = serverRequest.getServerId();

        log.info("Creating server: {} on port {}", serverId, serverRequest.getPort());

        // Create the server
        mockServerManager.createServer(serverRequest);

        log.info("Successfully created server: {}", serverId);

        // Configure expectations if present
        if (config.hasExpectations()) {
            log.info("Configuring {} expectation(s) for server: {}",
                config.getExpectations().size(), serverId);

            try {
                // Convert expectations list to JSON string
                String expectationsJson = objectMapper.writeValueAsString(config.getExpectations());

                // Get the server instance and configure expectations
                MockServerManager.ServerInstance serverInstance =
                    mockServerManager.getServerInstance(serverId);

                // Parse and configure expectations (similar to ExpectationController logic)
                configureExpectations(serverInstance, expectationsJson);

                log.info("Successfully configured expectations for server: {}", serverId);
            } catch (Exception e) {
                log.error("Failed to configure expectations for server: {}", serverId, e);
                // Server is already created, so we continue despite expectation failures
            }
        }
    }

    /**
     * Configures expectations for a server instance.
     * <p>
     * This method handles the parsing and configuration of expectations,
     * including merging global headers with expectation-specific headers.
     * </p>
     *
     * @param serverInstance the server instance to configure
     * @param expectationsJson JSON string containing the expectations
     */
    private void configureExpectations(
        MockServerManager.ServerInstance serverInstance,
        String expectationsJson
    ) {
        MockServerOperations mockServerOperations =
            new MockServerOperationsImpl(serverInstance.getServer());

        // Parse expectations from JSON
        org.mockserver.serialization.ExpectationSerializer serializer =
            new org.mockserver.serialization.ExpectationSerializer(
                new org.mockserver.logging.MockServerLogger());

        org.mockserver.mock.Expectation[] expectations;
        try {
            expectations = serializer.deserializeArray(expectationsJson, false);
        } catch (Exception e) {
            log.error("Failed to parse expectations", e);
            throw new ServerCreationException("Failed to parse expectations: " + e.getMessage(), e);
        }

        // Apply global headers and configure expectations
        List<com.example.mockserver.model.GlobalHeader> globalHeaders =
            serverInstance.getGlobalHeaders();

        if (globalHeaders != null && !globalHeaders.isEmpty()) {
            log.debug("Applying {} global headers to expectations", globalHeaders.size());
            for (org.mockserver.mock.Expectation expectation : expectations) {
                org.mockserver.mock.Expectation mergedExpectation =
                    applyGlobalHeaders(expectation, globalHeaders);
                mockServerOperations.configureExpectation(
                    mergedExpectation.getHttpRequest(),
                    mergedExpectation.getHttpResponse()
                );
            }
        } else {
            // No global headers, configure as-is
            for (org.mockserver.mock.Expectation expectation : expectations) {
                mockServerOperations.configureExpectation(
                    expectation.getHttpRequest(),
                    expectation.getHttpResponse()
                );
            }
        }
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
    private org.mockserver.mock.Expectation applyGlobalHeaders(
        org.mockserver.mock.Expectation expectation,
        List<com.example.mockserver.model.GlobalHeader> globalHeaders
    ) {
        org.mockserver.model.HttpResponse originalResponse = expectation.getHttpResponse();

        // Get existing headers from expectation
        List<org.mockserver.model.Header> existingHeaders =
            originalResponse.getHeaderList() != null
                ? new java.util.ArrayList<>(originalResponse.getHeaderList())
                : new java.util.ArrayList<>();

        // Convert existing headers to map for easy lookup
        java.util.Map<org.mockserver.model.NottableString, org.mockserver.model.Header> headerMap =
            existingHeaders.stream().collect(java.util.stream.Collectors.toMap(
                org.mockserver.model.Header::getName,
                h -> h,
                (h1, h2) -> h1  // Keep first if duplicate
            ));

        // Add global headers (only if not already present in expectation)
        for (com.example.mockserver.model.GlobalHeader globalHeader : globalHeaders) {
            org.mockserver.model.NottableString headerName =
                org.mockserver.model.NottableString.string(globalHeader.getName());
            if (!headerMap.containsKey(headerName)) {
                headerMap.put(
                    headerName,
                    org.mockserver.model.Header.header(globalHeader.getName(), globalHeader.getValue())
                );
                log.trace("Added global header: {} = {}",
                    globalHeader.getName(), globalHeader.getValue());
            } else {
                log.trace("Expectation header overrides global: {}", globalHeader.getName());
            }
        }

        // Create new header array from merged map
        org.mockserver.model.Header[] mergedHeaders =
            headerMap.values().toArray(new org.mockserver.model.Header[0]);

        // Create new response with merged headers
        org.mockserver.model.HttpResponse mergedResponse =
            originalResponse.withHeaders(mergedHeaders);

        // Return new expectation with merged response
        return new org.mockserver.mock.Expectation(expectation.getHttpRequest())
            .thenRespond(mergedResponse);
    }
}
