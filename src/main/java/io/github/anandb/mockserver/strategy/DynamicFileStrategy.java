package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.service.FreemarkerTemplateService;
import io.github.anandb.mockserver.util.FreemarkerTemplateDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Strategy for handling dynamic responses involving files or templates.
 */
@Slf4j
@Component
public class DynamicFileStrategy implements ResponseStrategy {

    private final FreemarkerTemplateService templateService;

    public DynamicFileStrategy(FreemarkerTemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public HttpResponse handle(HttpRequest request, EnhancedExpectationDTO config, Map<String, Object> context) {
        String pathPattern = (String) context.get("pathPattern");

        if (config.isFileResponse()) {
            return handleFileResponse(request, config, pathPattern);
        } else {
            return handleTemplateResponseBody(request, config, pathPattern);
        }
    }

    private HttpResponse handleFileResponse(HttpRequest request, EnhancedExpectationDTO config, String pathPattern) {
        String filePathTemplate = config.getFile();
        try {
            // 1. Evaluate file path template
            String filePath = evaluateFilePathTemplate(filePathTemplate, request, pathPattern);

            // 2. Find file with prefix (glob logic)
            File file = findFirstFileWithPrefix(filePath);
            if (file == null) {
                log.error("File not found using prefix: {}", filePath);
                return HttpResponse.response().withStatusCode(404).withBody("File not found: " + filePath);
            }

            // 3. Serve file
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String fileName = file.getName();
            String contentType = determineContentType(file.toPath());

            return HttpResponse.response()
                    .withStatusCode(config.getHttpResponse().getStatusCode())
                    .withHeaders(config.getHttpResponse().getHeaderList())
                    .withContentType(MediaType.parse(contentType))
                    .withHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .withBody(fileContent);

        } catch (Exception e) {
            log.error("Error creating file response", e);
            return HttpResponse.response().withStatusCode(500).withBody("Error creating file response: " + e.getMessage());
        }
    }

    private HttpResponse handleTemplateResponseBody(HttpRequest request, EnhancedExpectationDTO config, String pathPattern) {
        try {
            String templateString = config.getHttpResponse().getBodyAsString();
            String processedBody = templateService.processTemplateWithRequest(templateString, request, pathPattern);

            return HttpResponse.response()
                    .withStatusCode(config.getHttpResponse().getStatusCode())
                    .withHeaders(config.getHttpResponse().getHeaderList())
                    .withBody(processedBody);
        } catch (Exception e) {
            log.error("Error processing Freemarker template", e);
            return HttpResponse.response().withStatusCode(500).withBody("Error processing template: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(EnhancedExpectationDTO config) {
        // Support if 'file' field is present OR if the body is a Freemarker template
        if (config.isFileResponse()) {
            return true;
        }
        if (config.getHttpResponse() != null) {
            String body = config.getHttpResponse().getBodyAsString();
            return body != null && FreemarkerTemplateDetector.isFreemarkerTemplate(body);
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    private String evaluateFilePathTemplate(String filePathTemplate, HttpRequest httpRequest, String pathPattern) {
        try {
            if (FreemarkerTemplateDetector.isFreemarkerTemplate(filePathTemplate)) {
                String evaluatedPath = templateService.processTemplateWithRequest(filePathTemplate, httpRequest, pathPattern);
                return evaluatedPath.strip();
            }
            return filePathTemplate.strip();
        } catch (Exception e) {
            log.warn("Failed to evaluate file path template '{}', using as-is: {}", filePathTemplate, e.getMessage());
            return filePathTemplate.strip();
        }
    }

    private File findFirstFileWithPrefix(String filePathPrefix) {
        try {
            int lastSlash = filePathPrefix.lastIndexOf('/');
            String baseDir = lastSlash != -1 ? filePathPrefix.substring(0, lastSlash) : ".";
            String fileNamePrefix = lastSlash != -1 ? filePathPrefix.substring(lastSlash + 1) : filePathPrefix;

            try (Stream<Path> paths = Files.walk(Paths.get(baseDir))) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = ObjectUtils.getIfNull(path.getFileName(), () -> "").toString();
                            return name.startsWith(fileNamePrefix);
                        })
                        .map(Path::toFile)
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.debug("Error searching for file with prefix: {}", filePathPrefix, e);
            return null;
        }
    }

    private String determineContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType != null) return contentType;
        } catch (IOException e) {
            log.warn("Could not probe content type for file: {}", filePath, e);
        }
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".zip")) return "application/zip";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".xml")) return "application/xml";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".csv")) return "text/csv";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
