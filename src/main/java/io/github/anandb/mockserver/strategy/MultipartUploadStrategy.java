package io.github.anandb.mockserver.strategy;

import io.github.anandb.mockserver.model.EnhancedExpectation;
import io.github.anandb.mockserver.model.FileUploadConfig;
import io.github.anandb.mockserver.service.FreemarkerTemplateService;
import io.github.anandb.mockserver.util.FreemarkerTemplateDetector;
import io.github.anandb.mockserver.util.MultipartParser;

import lombok.extern.slf4j.Slf4j;

import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Strategy for handling multipart file upload requests.
 * <p>
 * Parses multipart/form-data bodies, extracts file parts, and saves them to a
 * configured directory. Path traversal in file names is rejected.
 * </p>
 */
@Slf4j
@Component
public class MultipartUploadStrategy implements ResponseStrategy {

    private final FreemarkerTemplateService templateService;

    public MultipartUploadStrategy(FreemarkerTemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public HttpResponse handle(HttpRequest request, EnhancedExpectation config, Map<String, Object> context) {
        String pathPattern = (String) context.get("pathPattern");
        FileUploadConfig uploadConfig = config.getFileUploadsConfig();

        if (uploadConfig == null || uploadConfig.getSaveTo() == null) {
            log.error("MultipartUploadStrategy invoked but no fileUploads config present");
            return HttpResponse.response().withStatusCode(500).withBody("No upload configuration");
        }

        try {
            // 1. Extract and save uploaded files
            String contentType = null;
            if (request.getHeaders() != null) {
                contentType = request.getHeaders().getEntries().stream()
                    .filter(h -> "Content-Type".equalsIgnoreCase(h.getName().getValue()))
                    .map(h -> h.getValues().getFirst().getValue())
                    .findFirst()
                    .orElse(null);
            }

            if (contentType == null || !contentType.toLowerCase().contains("multipart/")) {
                log.warn("Request to upload endpoint is not multipart: {}", contentType);
                return HttpResponse.response().withStatusCode(400).withBody("Expected multipart/form-data");
            }

            byte[] body = request.getBodyAsRawBytes();
            if (body == null || body.length == 0) {
                return HttpResponse.response().withStatusCode(400).withBody("Empty request body");
            }

            List<MultipartParser.Part> parts = MultipartParser.parse(body, contentType);

            // 2. Evaluate saveTo path template
            String saveToPath = uploadConfig.getSaveTo();
            if (FreemarkerTemplateDetector.isFreemarkerTemplate(saveToPath)) {
                saveToPath = templateService.processTemplateWithRequest(saveToPath, request, pathPattern);
            }
            saveToPath = saveToPath.strip();

            // 3. Create save directory
            Path saveDir = Paths.get(saveToPath);
            Files.createDirectories(saveDir);

            // 4. Save file parts
            int savedCount = 0;
            List<String> allowedFields = uploadConfig.getFileFields();

            for (MultipartParser.Part part : parts) {
                if (part.getFileName() == null) {
                    continue; // Skip non-file parts
                }
                if (allowedFields != null && !allowedFields.isEmpty()
                        && !allowedFields.contains(part.getFieldName())) {
                    continue; // Field not in allow-list
                }

                Path filePath = saveDir.resolve(part.getFileName());
                Files.write(filePath, part.getContent());
                savedCount++;
                log.info("Saved uploaded file: {} ({} bytes) from field '{}'",
                        filePath, part.getContent().length, part.getFieldName());
            }

            log.info("Multipart upload complete: {} files saved to {}", savedCount, saveToPath);

            // 5. Return configured response
            if (config.getHttpResponse() != null) {
                return HttpResponse.response()
                        .withStatusCode(config.getHttpResponse().getStatusCode())
                        .withHeaders(config.getHttpResponse().getHeaderList())
                        .withBody(config.getHttpResponse().getBodyAsString());
            }
            return HttpResponse.response().withStatusCode(200).withBody("Uploaded " + savedCount + " files");

        } catch (IllegalArgumentException e) {
            log.warn("Multipart upload rejected: {}", e.getMessage());
            return HttpResponse.response().withStatusCode(400).withBody("Upload rejected: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to save uploaded files", e);
            return HttpResponse.response().withStatusCode(500).withBody("Failed to save files: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing multipart upload", e);
            return HttpResponse.response().withStatusCode(500).withBody("Upload error: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(EnhancedExpectation config) {
        return config.hasFileUploads();
    }

    @Override
    public int getPriority() {
        return 15; // Above DynamicFileStrategy (10)
    }
}
