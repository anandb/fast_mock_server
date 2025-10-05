package com.example.mockserver.callback;

import lombok.extern.slf4j.Slf4j;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Callback for serving multi-part file downloads.
 * <p>
 * This callback is invoked by MockServer when a request matches an expectation
 * configured with file paths. It reads the specified files and returns them as
 * a multi-part response.
 * </p>
 */
@Slf4j
public class FileResponseCallback implements ExpectationResponseCallback {

    private final List<String> filePaths;
    private final HttpResponse baseResponse;

    public FileResponseCallback(List<String> filePaths, HttpResponse baseResponse) {
        this.filePaths = filePaths;
        this.baseResponse = baseResponse;
    }

    @Override
    public HttpResponse handle(HttpRequest httpRequest) {
        try {
            // Generate a boundary for multipart response
            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");

            // Build multipart body using ByteArrayOutputStream to handle binary data safely
            ByteArrayOutputStream multipartBody = new ByteArrayOutputStream();

            for (String filePath : filePaths) {
                File file = new File(filePath);

                if (!file.exists()) {
                    log.error("File not found: {}", filePath);
                    return HttpResponse.response()
                            .withStatusCode(404)
                            .withBody("File not found: " + filePath);
                }

                if (!file.isFile()) {
                    log.error("Path is not a file: {}", filePath);
                    return HttpResponse.response()
                            .withStatusCode(400)
                            .withBody("Path is not a file: " + filePath);
                }

                try {
                    // Read file content
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    String fileName = file.getName();
                    String contentType = determineContentType(file.toPath());

                    // Add multipart section - write headers as text
                    multipartBody.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                    multipartBody.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" +
                            fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                    multipartBody.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
                    multipartBody.write("\r\n".getBytes(StandardCharsets.UTF_8));

                    // Write binary file content directly without String conversion
                    multipartBody.write(fileContent);
                    multipartBody.write("\r\n".getBytes(StandardCharsets.UTF_8));

                    log.debug("Added file to multipart response: {} ({} bytes)", fileName, fileContent.length);
                } catch (IOException e) {
                    log.error("Error reading file: {}", filePath, e);
                    return HttpResponse.response()
                            .withStatusCode(500)
                            .withBody("Error reading file: " + filePath + " - " + e.getMessage());
                }
            }

            // Add final boundary
            multipartBody.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            // Return response with multipart body as binary data
            HttpResponse response = HttpResponse.response()
                    .withStatusCode(baseResponse.getStatusCode())
                    .withHeaders(baseResponse.getHeaderList())
                    .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .withBody(multipartBody.toByteArray());

            log.info("Served multipart response with {} file(s)", filePaths.size());
            return response;

        } catch (Exception e) {
            log.error("Error creating multipart file response", e);
            return HttpResponse.response()
                    .withStatusCode(500)
                    .withBody("Error creating file response: " + e.getMessage());
        }
    }

    /**
     * Determines the content type of a file based on its extension.
     *
     * @param filePath the path to the file
     * @return the content type as a string
     */
    private String determineContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType != null) {
                return contentType;
            }
        } catch (IOException e) {
            log.warn("Could not probe content type for file: {}", filePath, e);
        }

        // Fallback to common types based on extension
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".zip")) {
            return "application/zip";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".xml")) {
            return "application/xml";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".csv")) {
            return "text/csv";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        }

        // Default to binary
        return "application/octet-stream";
    }
}
