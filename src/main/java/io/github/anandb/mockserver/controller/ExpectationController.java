package io.github.anandb.mockserver.controller;

import io.github.anandb.mockserver.exception.InvalidExpectationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.service.MockServerManager;
import io.github.anandb.mockserver.service.MockServerManager.ServerInstance;
import io.github.anandb.mockserver.service.MockServerOperations;
import io.github.anandb.mockserver.service.MockServerOperationsImpl;
import io.github.anandb.mockserver.service.FreemarkerTemplateService;
import io.github.anandb.mockserver.callback.FreemarkerResponseCallback;
import io.github.anandb.mockserver.callback.FileResponseCallback;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.util.FreemarkerTemplateDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
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

    private final FreemarkerTemplateService freemarkerTemplateService;
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

        try {
            // Use custom parsing that allows flexible structures and bypasses MockServer validation
            Expectation[] expectations = parseExpectationsCustomOnly(expectationsJson);
            log.debug("Parsed {} expectations", expectations.length);

            // Extract files information before parsing (since MockServer doesn't know about files field)
            Map<String, List<String>> filesMap = extractFilesFromJson(expectationsJson);

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

                // Clear any existing expectations with same method and path to allow overwriting
                clearMatchingExpectation(server, processedExpectation.getHttpRequest());

                // Check if this expectation has files field (for multi-part file downloads)
                String expectationKey = generateExpectationKey(processedExpectation.getHttpRequest());
                List<String> filePaths = filesMap.get(expectationKey);

                if (filePaths != null && !filePaths.isEmpty()) {
                    log.debug("Detected files field in expectation, configuring file callback");
                    configureFileExpectation(
                        server,
                        processedExpectation.getHttpRequest(),
                        processedExpectation.getHttpResponse(),
                        filePaths
                    );
                } else {
                    // Check if response body contains Freemarker template
                    HttpResponse response = processedExpectation.getHttpResponse();
                    String responseBody = response.getBodyAsString();

                    if (responseBody != null && FreemarkerTemplateDetector.isFreemarkerTemplate(responseBody)) {
                        log.debug("Detected Freemarker template in response body, configuring callback");
                        configureTemplateExpectation(
                            server,
                            processedExpectation.getHttpRequest(),
                            response,
                            responseBody
                        );
                    } else {
                        // Regular static response
                        server.when(processedExpectation.getHttpRequest()).respond(processedExpectation.getHttpResponse());
                    }
                }
            }

            String message = String.format(
                "Successfully configured %d expectation(s) for server: %s",
                expectations.length,
                serverId
            );
            log.info(message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Failed to configure expectations", e);
            throw new InvalidExpectationException("Failed to configure expectations: " + e.getMessage(), e);
        }
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
        io.github.anandb.mockserver.model.BasicAuthConfig basicAuthConfig
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
        Header[] mergedHeaders = headerMap.values().toArray(Header[]::new);

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
     * Custom expectation parsing that completely bypasses MockServer validation.
     * <p>
     * This method allows for flexible httpResponse structures and stores any custom
     * fields as headers with an "X-Custom-" prefix.
     * </p>
     *
     * @param json the raw JSON string containing expectations
     * @return array of Expectation objects
     * @throws InvalidExpectationException if the JSON cannot be parsed
     */
    private Expectation[] parseExpectationsCustomOnly(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            List<Expectation> expectations = new ArrayList<>();

            // Handle both single expectation and array
            List<JsonNode> expectationNodes = new ArrayList<>();
            if (rootNode.isArray()) {
                rootNode.forEach(expectationNodes::add);
            } else {
                expectationNodes.add(rootNode);
            }

            for (JsonNode expectationNode : expectationNodes) {
                JsonNode httpRequestNode = expectationNode.get("httpRequest");
                JsonNode httpResponseNode = expectationNode.get("httpResponse");

                // Validate that both httpRequest and httpResponse are present
                if (httpRequestNode == null) {
                    throw new InvalidExpectationException("Missing required field: httpRequest");
                }
                if (httpResponseNode == null) {
                    throw new InvalidExpectationException("Missing required field: httpResponse");
                }

                // Create minimal HttpRequest
                HttpRequest request = createMinimalHttpRequest(httpRequestNode);

                // Create HttpResponse allowing any structure
                HttpResponse response = createFlexibleHttpResponse(httpResponseNode);

                expectations.add(new Expectation(request).thenRespond(response));
            }

            return expectations.toArray(Expectation[]::new);
        } catch (Exception e) {
            log.error("Failed to parse expectations with custom parser", e);
            throw new InvalidExpectationException("Failed to parse expectations: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a minimal HttpRequest from JSON node.
     * <p>
     * Only extracts method and path for basic matching. Additional request
     * properties can be added as needed.
     * </p>
     *
     * @param requestNode the JSON node containing request information
     * @return HttpRequest object for matching
     */
    private HttpRequest createMinimalHttpRequest(JsonNode requestNode) {
        String method = "GET";
        String path = "/";

        if (requestNode.has("method") && requestNode.get("method").isTextual()) {
            method = requestNode.get("method").asText();
        }
        if (requestNode.has("path") && requestNode.get("path").isTextual()) {
            path = requestNode.get("path").asText();
        }

        return request().withMethod(method).withPath(path);
    }

    /**
     * Creates HttpResponse from JSON node, allowing any custom structure.
     * <p>
     * Standard fields (statusCode, body, headers) are handled appropriately.
     * Any additional fields are stored as custom headers with "X-Custom-" prefix.
     * </p>
     *
     * @param responseNode the JSON node containing response information
     * @return HttpResponse object
     */
    private HttpResponse createFlexibleHttpResponse(JsonNode responseNode) {
        // Collect all the data we need to build the response
        final Integer statusCode = getStatusCode(responseNode);
        final String body = getBody(responseNode);
        final List<Header> allHeaders = getAllHeaders(responseNode);

        // Build the response with all collected data
        HttpResponse response = HttpResponse.response().withStatusCode(statusCode);

        if (body != null) {
            response = response.withBody(body);
        }

        if (!allHeaders.isEmpty()) {
            response = response.withHeaders(allHeaders.toArray(Header[]::new));
        }

        return response;
    }

    /**
     * Extracts status code from response node.
     */
    private Integer getStatusCode(JsonNode responseNode) {
        JsonNode statusNode = responseNode.get("statusCode");
        return (statusNode != null && statusNode.isNumber()) ? statusNode.asInt() : 200;
    }

    /**
     * Extracts body from response node.
     */
    private String getBody(JsonNode responseNode) {
        JsonNode bodyNode = responseNode.get("body");
        if (bodyNode == null) {
            return null;
        }
        return bodyNode.isTextual() ? bodyNode.asText() : bodyNode.toString();
    }

    /**
     * Extracts all headers from response node, including custom fields.
     */
    private List<Header> getAllHeaders(JsonNode responseNode) {
        List<Header> allHeaders = new ArrayList<>();

        // Handle standard headers field
        JsonNode headersNode = responseNode.get("headers");
        if (headersNode != null && headersNode.isArray()) {
            headersNode.forEach(headerNode -> {
                if (headerNode.has("name") && headerNode.has("value")) {
                    allHeaders.add(header(
                        headerNode.get("name").asText(),
                        headerNode.get("value").asText()
                    ));
                }
            });
        }

        // Handle other fields as custom headers
        responseNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();

            // Skip standard fields we've already handled
            if (!fieldName.equals("statusCode") && !fieldName.equals("body") && !fieldName.equals("headers")) {
                String value = fieldValue.isTextual() ? fieldValue.asText() : fieldValue.toString();
                allHeaders.add(header("X-Custom-" + fieldName, value));
            }
        });

        return allHeaders;
    }

    /**
     * Clears any existing expectations that match the same HTTP method and path.
     * This allows new expectations to overwrite previous ones for the same endpoint.
     *
     * @param server the MockServer instance
     * @param request the HTTP request to match (method and path)
     */
    private void clearMatchingExpectation(ClientAndServer server, org.mockserver.model.RequestDefinition request) {
        if (request instanceof HttpRequest httpRequest) {
            // Create a matcher for the same method and path
            HttpRequest matcher = request()
                    .withMethod(httpRequest.getMethod())
                    .withPath(httpRequest.getPath());

            // Clear expectations matching this method and path
            server.clear(matcher);
            log.debug("Cleared existing expectations for {} {}",
                     httpRequest.getMethod(), httpRequest.getPath());
        }
    }

    /**
     * Extracts files information from expectation JSON before parsing.
     * This is necessary because MockServer's serializer doesn't know about our custom "files" field.
     *
     * @param json the raw JSON string
     * @return map of expectation keys to file path lists
     */
    private Map<String, List<String>> extractFilesFromJson(String json) {
        Map<String, List<String>> filesMap = new HashMap<>();

        try {
            JsonNode rootNode = objectMapper.readTree(json);

            // Handle both single expectation and array
            List<JsonNode> expectationNodes = new ArrayList<>();
            if (rootNode.isArray()) {
                rootNode.forEach(expectationNodes::add);
            } else {
                expectationNodes.add(rootNode);
            }

            // Extract files from each expectation
            for (JsonNode expectationNode : expectationNodes) {
                JsonNode httpRequest = expectationNode.get("httpRequest");
                JsonNode httpResponse = expectationNode.get("httpResponse");

                if (httpRequest != null && httpResponse != null) {
                    JsonNode filesNode = httpResponse.get("files");

                    if (filesNode != null && filesNode.isArray()) {
                        List<String> filePaths = new ArrayList<>();
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
        } catch (Exception e) {
            log.error("Error extracting files from JSON", e);
        }

        return filesMap;
    }

    /**
     * Generates a unique key for an expectation based on its HTTP request.
     *
     * @param request the HTTP request
     * @return a unique key string
     */
    private String generateExpectationKey(org.mockserver.model.RequestDefinition request) {
        if (request instanceof HttpRequest httpRequest) {
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
     * @param response the base HTTP response (contains headers and status)
     * @param filePaths list of absolute file paths to serve
     */
    private void configureFileExpectation(
            ClientAndServer server,
            org.mockserver.model.RequestDefinition request,
            HttpResponse response,
            List<String> filePaths) {
        String pathPattern = null;
        if (request instanceof org.mockserver.model.HttpRequest httpRequest && httpRequest.getPath() != null) {
            pathPattern = httpRequest.getPath().getValue();
        }
        // Create callback with file paths and template service for dynamic file paths
        FileResponseCallback callback = new FileResponseCallback(
            filePaths,
            response,
            freemarkerTemplateService,
            pathPattern
        );

        // Configure expectation with callback
        server.when(request).respond(callback);
    }

    /**
     * Configures an expectation with a Freemarker template callback.
     *
     * @param server the MockServer instance
     * @param request the HTTP request matcher
     * @param response the base HTTP response (contains headers and status)
     * @param templateString the Freemarker template string
     */
    private void configureTemplateExpectation(
            ClientAndServer server,
            org.mockserver.model.RequestDefinition request,
            HttpResponse response,
            String templateString) {

        // Extract path pattern from request for path variable matching
        String pathPattern = null;
        if (request instanceof HttpRequest httpRequest && httpRequest.getPath() != null) {
            pathPattern = httpRequest.getPath().getValue();
        }

        // Create callback with template service and path pattern
        FreemarkerResponseCallback callback = new FreemarkerResponseCallback(
            freemarkerTemplateService,
            templateString,
            response,
            pathPattern
        );

        // Configure expectation with callback
        server.when(request).respond(callback);
    }

    /**
     * Retrieves all active expectations for a specific mock server.
     *
     * @param serverId the unique identifier of the server
     * @return ResponseEntity containing an array of active expectations
     * @throws ServerNotFoundException if the server with the specified ID is not found
     */
    @GetMapping(value = "/{serverId}/expectations", produces = "application/json")
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
