package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.callback.FileResponseCallback;
import io.github.anandb.mockserver.callback.FreemarkerResponseCallback;
import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.ServerConfiguration;
import io.github.anandb.mockserver.util.JsonCommentParser;
import io.github.anandb.mockserver.util.FreemarkerTemplateDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
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
    private final FreemarkerTemplateService freemarkerTemplateService;

    /** System property name for specifying the configuration file path */
    private static final String CONFIG_FILE_PROPERTY = "mock.server.config.file";

    /** System property name for specifying base64-encoded configuration content */
    private static final String CONFIG_FILE_B64_PROPERTY = "mock.server.config.fileb64";

    /**
     * Loads server configurations from JSON file or base64-encoded content on application startup.
     * <p>
     * This method is automatically called after the Spring context is initialized.
     * It first checks for base64-encoded configuration (mock.server.config.fileb64),
     * then falls back to file path (mock.server.config.file) if not found.
     * If neither property is set, the service will log a message and continue
     * without loading any configurations.
     * </p>
     */
    @PostConstruct
    public void loadConfigurationsOnStartup() {
        // First check for base64-encoded configuration
        String configB64 = System.getProperty(CONFIG_FILE_B64_PROPERTY);

        if (configB64 != null && !configB64.trim().isEmpty()) {
            log.info("Loading server configurations from base64-encoded content");

            try {
                loadConfigurationsFromBase64(configB64);
                return;
            } catch (Exception e) {
                log.error("Failed to load configurations from base64-encoded content", e);
                throw new ServerCreationException(
                    "Failed to load configurations from base64-encoded content: " + e.getMessage(), e);
            }
        }

        // Fall back to file-based configuration
        String configFilePath = System.getProperty(CONFIG_FILE_PROPERTY);

        if (configFilePath == null || configFilePath.trim().isEmpty()) {
            log.info("No configuration specified. Use -D{}=<path> to load servers from a JSON file, " +
                "or -D{}=<base64> to load from base64-encoded content.",
                CONFIG_FILE_PROPERTY, CONFIG_FILE_B64_PROPERTY);
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
            loadConfigurationsFromFile(configFile);
        } catch (Exception e) {
            log.error("Failed to load configurations from file: {}", configFilePath, e);
            throw new ServerCreationException(
                "Failed to load configurations from file: " + configFilePath, e);
        }
    }

    /**
     * Loads and processes all server configurations from base64-encoded content.
     * Automatically detects if content is JSONMC format (starts with "/*" or contains comments).
     *
     * @param configB64 the base64-encoded configuration content
     * @throws IOException if the content cannot be decoded or parsed
     */
    private void loadConfigurationsFromBase64(String configB64) throws IOException {
        // Decode base64 content
        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(configB64.trim());
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 encoding in configuration", e);
            throw new IOException("Invalid base64 encoding: " + e.getMessage(), e);
        }

        String configContent = new String(decodedBytes, StandardCharsets.UTF_8);
        log.debug("Decoded configuration content ({} bytes)", configContent.length());

        loadConfigurationsFromString(configContent, true);
    }

    /**
     * Loads and processes all server configurations from the specified file.
     * If the file has a .jsonmc extension, it will be processed through JsonCommentParser
     * to handle comments and multiline strings before parsing.
     *
     * @param configFile the JSON configuration file to read
     * @throws IOException if the file cannot be read or parsed
     */
    private void loadConfigurationsFromFile(File configFile) throws IOException {
        // Check if file has .jsonmc extension
        String fileName = configFile.getName();
        boolean isJsonmc = fileName.toLowerCase().endsWith(".jsonmc");

        // Read file content as string
        String fileContent = Files.readString(configFile.toPath());

        loadConfigurationsFromString(fileContent, isJsonmc);
    }

    /**
     * Loads and processes all server configurations from a string.
     * If isJsonmc is true or content appears to be JSONMC format, it will be processed
     * through JsonCommentParser to handle comments and multiline strings before parsing.
     *
     * @param configContent the configuration content as a string
     * @param isJsonmc whether the content should be treated as JSONMC format
     * @throws IOException if the content cannot be parsed
     */
    private void loadConfigurationsFromString(String configContent, boolean isJsonmc) throws IOException {
        ServerConfiguration[] configurations;

        // Auto-detect JSONMC format if not explicitly specified
        if (!isJsonmc && (configContent.trim().startsWith("/*") || configContent.contains("//"))) {
            log.debug("Auto-detected JSONMC format based on content");
            isJsonmc = true;
        }

        if (isJsonmc) {
            log.info("Processing content as JSONMC format with JsonCommentParser");

            // Clean the JSON (remove comments and convert multiline strings)
            String cleanedJson = JsonCommentParser.clean(configContent);

            // Parse the cleaned JSON with ObjectMapper
            configurations = objectMapper.readValue(
                cleanedJson,
                ServerConfiguration[].class
            );
        } else {
            // Standard JSON processing
            configurations = objectMapper.readValue(
                configContent,
                ServerConfiguration[].class
            );
        }

        if (configurations == null || configurations.length == 0) {
            log.warn("No server configurations found in content");
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
     * including merging global headers with expectation-specific headers,
     * and handling file-based responses and Freemarker templates.
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

        // Extract files information before parsing
        java.util.Map<String, List<String>> filesMap = extractFilesFromJson(expectationsJson);

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
        List<io.github.anandb.mockserver.model.GlobalHeader> globalHeaders =
            serverInstance.getGlobalHeaders();

        for (org.mockserver.mock.Expectation expectation : expectations) {
            // Apply global headers if present
            org.mockserver.mock.Expectation processedExpectation = expectation;
            if (globalHeaders != null && !globalHeaders.isEmpty()) {
                log.debug("Applying {} global headers to expectation", globalHeaders.size());
                processedExpectation = applyGlobalHeaders(expectation, globalHeaders);
            }

            // Check if this expectation has files field
            String expectationKey = generateExpectationKey(processedExpectation.getHttpRequest());
            List<String> filePaths = filesMap.get(expectationKey);

            if (filePaths != null && !filePaths.isEmpty()) {
                log.debug("Detected files field in expectation, configuring file callback");
                configureFileExpectation(
                    serverInstance.getServer(),
                    processedExpectation.getHttpRequest(),
                    processedExpectation.getHttpResponse(),
                    filePaths
                );
            } else {
                // Check if response body contains Freemarker template
                org.mockserver.model.HttpResponse response = processedExpectation.getHttpResponse();
                String responseBody = response.getBodyAsString();

                if (responseBody != null && FreemarkerTemplateDetector.isFreemarkerTemplate(responseBody)) {
                    log.debug("Detected Freemarker template in response body, configuring callback");
                    configureTemplateExpectation(
                        serverInstance.getServer(),
                        processedExpectation.getHttpRequest(),
                        response,
                        responseBody
                    );
                } else {
                    // Regular static response
                    mockServerOperations.configureExpectation(
                        processedExpectation.getHttpRequest(),
                        response
                    );
                }
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
        List<io.github.anandb.mockserver.model.GlobalHeader> globalHeaders
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
        for (io.github.anandb.mockserver.model.GlobalHeader globalHeader : globalHeaders) {
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
            headerMap.values().toArray(org.mockserver.model.Header[]::new);

        // Create new response with merged headers
        org.mockserver.model.HttpResponse mergedResponse =
            originalResponse.withHeaders(mergedHeaders);

        // Return new expectation with merged response
        return new org.mockserver.mock.Expectation(expectation.getHttpRequest())
            .thenRespond(mergedResponse);
    }

    /**
     * Extracts files information from expectation JSON before parsing.
     *
     * @param json the raw JSON string
     * @return map of expectation keys to file path lists
     */
    private java.util.Map<String, List<String>> extractFilesFromJson(String json) {
        java.util.Map<String, List<String>> filesMap = new java.util.HashMap<>();

        try {
            JsonNode rootNode = objectMapper.readTree(json);

            // Handle array of expectations
            if (rootNode.isArray()) {
                for (JsonNode expectationNode : rootNode) {
                    extractFilesFromExpectationNode(expectationNode, filesMap);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting files from JSON", e);
        }

        return filesMap;
    }

    /**
     * Extracts files from a single expectation node.
     *
     * @param expectationNode the expectation JSON node
     * @param filesMap the map to store extracted file paths
     */
    private void extractFilesFromExpectationNode(
        JsonNode expectationNode,
        java.util.Map<String, List<String>> filesMap
    ) {
        JsonNode httpRequest = expectationNode.get("httpRequest");
        JsonNode httpResponse = expectationNode.get("httpResponse");

        if (httpRequest != null && httpResponse != null) {
            JsonNode filesNode = httpResponse.get("files");

            if (filesNode != null && filesNode.isArray()) {
                List<String> filePaths = new java.util.ArrayList<>();
                filesNode.forEach(fileNode -> {
                    if (fileNode.isTextual()) {
                        filePaths.add(fileNode.asText());
                    }
                });

                if (!filePaths.isEmpty()) {
                    // Generate key from request method and path
                    String method = httpRequest.has("method") ?
                        httpRequest.get("method").asText() : "GET";
                    String path = httpRequest.has("path") ?
                        httpRequest.get("path").asText() : "/";
                    String key = method + ":" + path;

                    filesMap.put(key, filePaths);
                    log.debug("Extracted {} file(s) for {} {}", filePaths.size(), method, path);
                }
            }
        }
    }

    /**
     * Generates a unique key for an expectation based on its HTTP request.
     *
     * @param request the HTTP request
     * @return a unique key string
     */
    private String generateExpectationKey(org.mockserver.model.RequestDefinition request) {
        if (request instanceof org.mockserver.model.HttpRequest httpRequest) {
            String method = httpRequest.getMethod() != null ?
                httpRequest.getMethod().getValue() : "GET";
            String path = httpRequest.getPath() != null ?
                httpRequest.getPath().getValue() : "/";
            return method + ":" + path;
        }
        return "UNKNOWN";
    }

    /**
     * Configures an expectation with a file response callback.
     *
     * @param server the MockServer instance
     * @param request the HTTP request matcher
     * @param response the base HTTP response
     * @param filePaths list of absolute file paths to serve
     */
    private void configureFileExpectation(
            org.mockserver.integration.ClientAndServer server,
            org.mockserver.model.RequestDefinition request,
            org.mockserver.model.HttpResponse response,
            List<String> filePaths) {

        FileResponseCallback callback = new FileResponseCallback(
            filePaths,
            response,
            freemarkerTemplateService
        );
        server.when(request).respond(callback);
    }

    /**
     * Configures an expectation with a Freemarker template callback.
     *
     * @param server the MockServer instance
     * @param request the HTTP request matcher
     * @param response the base HTTP response
     * @param templateString the Freemarker template string
     */
    private void configureTemplateExpectation(
            org.mockserver.integration.ClientAndServer server,
            org.mockserver.model.RequestDefinition request,
            org.mockserver.model.HttpResponse response,
            String templateString) {

        // Extract path pattern from request for path variable matching
        String pathPattern = null;
        if (request instanceof org.mockserver.model.HttpRequest httpRequest && httpRequest.getPath() != null) {
            pathPattern = httpRequest.getPath().getValue();
        }

        FreemarkerResponseCallback callback = new FreemarkerResponseCallback(
            freemarkerTemplateService,
            templateString,
            response,
            pathPattern
        );
        server.when(request).respond(callback);
    }
}
