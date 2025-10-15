package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.HttpRequestContext;
import io.github.anandb.mockserver.util.MapperSupplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import lombok.extern.slf4j.Slf4j;

import org.mockserver.model.Cookie;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for processing Freemarker templates with HTTP request context.
 * <p>
 * This service parses HTTP requests into a context object containing headers, body,
 * and cookies, then evaluates Freemarker templates using this context.
 * </p>
 */
@Slf4j
@Service
public class FreemarkerTemplateService {

    private final Configuration freemarkerConfig;
    private final JsonMapper objectMapper;

    public FreemarkerTemplateService() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
        this.freemarkerConfig.setNumberFormat("0.######");
        this.objectMapper = MapperSupplier.getMapper();
    }

    /**
     * Parses an HTTP request into a context object for template evaluation.
     *
     * @param httpRequest the incoming HTTP request
     * @param pathPattern the expectation path pattern (e.g., "/users/{id}") for extracting path variables
     * @return HttpRequestContext containing headers, body, cookies, and path variables
     */
    public HttpRequestContext parseHttpRequest(HttpRequest httpRequest, String pathPattern) {
        // Parse headers - assuming one value per header
        Map<String, String> headers = new HashMap<>();
        if (httpRequest.getHeaderList() != null) {
            for (Header header : httpRequest.getHeaderList()) {
                // Get first value if multiple values exist
                if (header.getValues() != null && !header.getValues().isEmpty()) {
                    headers.put(header.getName().getValue(), header.getValues().get(0).getValue());
                }
            }
        }

        // Parse body as JSON
        JsonNode body;
        if (httpRequest.getBodyAsString() != null && !httpRequest.getBodyAsString().isEmpty()) {
            try {
                body = objectMapper.readTree(httpRequest.getBodyAsString());
            } catch (Exception e) {
                log.warn("Failed to parse request body as JSON: {}", e.getMessage());
                // If parsing fails, create empty JsonNode
                body = objectMapper.createObjectNode();
            }
        } else {
            body = objectMapper.createObjectNode();
        }

        // Parse cookies
        Map<String, String> cookies = new HashMap<>();
        if (httpRequest.getCookieList() != null) {
            for (Cookie cookie : httpRequest.getCookieList()) {
                cookies.put(cookie.getName().getValue(), cookie.getValue().getValue());
            }
        }

        // Extract path variables
        Map<String, String> pathVariables = extractPathVariables(
            httpRequest.getPath().getValue(),
            pathPattern
        );

        return HttpRequestContext.builder()
                .headers(headers)
                .body(body)
                .cookies(cookies)
                .pathVariables(pathVariables)
                .build();
    }

    /**
     * Extracts path variables from a request path based on a path pattern.
     * <p>
     * For example, given pattern "/users/{id}/posts/{postId}" and path "/users/123/posts/456",
     * this will return a map: {"id": "123", "postId": "456"}
     * </p>
     *
     * @param requestPath the actual request path
     * @param pathPattern the expectation path pattern with variables in {brackets}
     * @return map of path variable names to values
     */
    private Map<String, String> extractPathVariables(String requestPath, String pathPattern) {
        Map<String, String> pathVariables = new HashMap<>();

        if (pathPattern == null || requestPath == null) {
            return pathVariables;
        }

        // Split both paths into segments
        String[] patternSegments = pathPattern.split("/");
        String[] pathSegments = requestPath.split("/");

        // Must have same number of segments
        if (patternSegments.length != pathSegments.length) {
            log.debug("Path segment count mismatch: pattern has {} segments, request has {}",
                     patternSegments.length, pathSegments.length);
            return pathVariables;
        }

        // Match segments and extract variables
        for (int i = 0; i < patternSegments.length; i++) {
            String patternSegment = patternSegments[i];
            String pathSegment = pathSegments[i];

            // Check if this segment is a variable (enclosed in curly braces)
            if (patternSegment.startsWith("{") && patternSegment.endsWith("}")) {
                // Extract variable name (remove braces)
                String variableName = patternSegment.substring(1, patternSegment.length() - 1);
                pathVariables.put(variableName, pathSegment);
                log.trace("Extracted path variable: {} = {}", variableName, pathSegment);
            }
        }

        log.debug("Extracted {} path variable(s) from path", pathVariables.size());
        return pathVariables;
    }

    /**
     * Processes a Freemarker template string with the provided context.
     *
     * @param templateString the Freemarker template string
     * @param context the HTTP request context to use as data
     * @return the processed template result
     * @throws IOException if template processing fails
     * @throws TemplateException if template evaluation fails
     */
    public String processTemplate(String templateString, HttpRequestContext context)
            throws IOException, TemplateException {

        // Create template from string
        Template template = new Template(
            "response-template",
            new StringReader(templateString),
            freemarkerConfig
        );

        // Prepare data model for Freemarker
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("headers", context.getHeaders());
        dataModel.put("body", objectMapper.convertValue(context.getBody(), Map.class));
        dataModel.put("cookies", context.getCookies());
        dataModel.put("pathVariables", context.getPathVariables() != null ? context.getPathVariables() : new HashMap<>());

        System.out.println(dataModel.get("pathVariables"));

        // Process template
        StringWriter writer = new StringWriter();
        template.process(dataModel, writer);

        return writer.toString();
    }

    /**
     * Processes a Freemarker template with an HTTP request.
     * <p>
     * This is a convenience method that combines parsing and processing.
     * </p>
     *
     * @param templateString the Freemarker template string
     * @param httpRequest the incoming HTTP request
     * @param pathPattern the expectation path pattern for extracting path variables
     * @return the processed template result
     * @throws IOException if template processing fails
     * @throws TemplateException if template evaluation fails
     */
    public String processTemplateWithRequest(String templateString, HttpRequest httpRequest, String pathPattern)
            throws IOException, TemplateException {
        HttpRequestContext context = parseHttpRequest(httpRequest, pathPattern);
        return processTemplate(templateString, context);
    }
}
