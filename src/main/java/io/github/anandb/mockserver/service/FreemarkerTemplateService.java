package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.HttpRequestContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private final Gson gson;

    public FreemarkerTemplateService() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
        this.freemarkerConfig.setNumberFormat("0.######");
        this.gson = new Gson();
    }

    /**
     * Parses an HTTP request into a context object for template evaluation.
     *
     * @param httpRequest the incoming HTTP request
     * @return HttpRequestContext containing headers, body, and cookies
     */
    public HttpRequestContext parseHttpRequest(HttpRequest httpRequest) {
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
        JsonObject body = null;
        if (httpRequest.getBodyAsString() != null && !httpRequest.getBodyAsString().isEmpty()) {
            try {
                body = JsonParser.parseString(httpRequest.getBodyAsString()).getAsJsonObject();
            } catch (Exception e) {
                log.warn("Failed to parse request body as JSON: {}", e.getMessage());
                // If parsing fails, create empty JsonObject
                body = new JsonObject();
            }
        } else {
            body = new JsonObject();
        }

        // Parse cookies
        Map<String, String> cookies = new HashMap<>();
        if (httpRequest.getCookieList() != null) {
            for (Cookie cookie : httpRequest.getCookieList()) {
                cookies.put(cookie.getName().getValue(), cookie.getValue().getValue());
            }
        }

        return HttpRequestContext.builder()
                .headers(headers)
                .body(body)
                .cookies(cookies)
                .build();
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
        dataModel.put("body", gson.fromJson(context.getBody(), Map.class));
        dataModel.put("cookies", context.getCookies());

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
     * @return the processed template result
     * @throws IOException if template processing fails
     * @throws TemplateException if template evaluation fails
     */
    public String processTemplateWithRequest(String templateString, HttpRequest httpRequest)
            throws IOException, TemplateException {
        HttpRequestContext context = parseHttpRequest(httpRequest);
        return processTemplate(templateString, context);
    }
}
