package io.github.anandb.mockserver.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.anandb.mockserver.model.EnhancedExpectation;
import io.github.anandb.mockserver.service.FreemarkerTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultipartUploadStrategy Tests")
class MultipartUploadStrategyTest {

    @Mock
    private FreemarkerTemplateService templateService;

    private MultipartUploadStrategy strategy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        strategy = new MultipartUploadStrategy(templateService);
    }

    @Test
    void supportsReturnsTrueWhenFileUploadsPresent() {
        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", "/tmp/uploads");
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);

        assertTrue(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseWhenNoFileUploads() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertFalse(strategy.supports(config));
    }

    @Test
    void getPriorityReturns15() {
        assertEquals(15, strategy.getPriority());
    }

    @Test
    void handleReturns500WhenNoUploadConfig() {
        EnhancedExpectation config = new EnhancedExpectation();
        HttpRequest request = HttpRequest.request();

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBodyAsString().contains("No upload configuration"));
    }

    @Test
    void handleReturns400WhenContentTypeNotMultipart() {
        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", tempDir.toString());
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);

        HttpRequest request = HttpRequest.request()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"data\":\"test\"}");

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBodyAsString().contains("Expected multipart"));
    }

    @Test
    void handleReturns400WhenBodyIsEmpty() {
        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", tempDir.toString());
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);

        String boundary = "----TestBoundary";
        HttpRequest request = HttpRequest.request()
            .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary);

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBodyAsString().contains("Empty request body"));
    }

    @Test
    void handleSavesMultipartFileToDisk() throws Exception {
        String saveTo = tempDir.toString();
        String boundary = "----TestBoundary";

        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", saveTo);
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);

        String bodyStr = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"upload\"; filename=\"data.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "file-content-here\r\n" +
            "--" + boundary + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.request()
            .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
            .withBody(body);

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBodyAsString().contains("Uploaded 1"));
    }

    @Test
    void handleReturns400OnPathTraversalInFileName() {
        String saveTo = tempDir.toString();
        String boundary = "----TestBoundary";

        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", saveTo);
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);

        String bodyStr = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"../../etc/passwd\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "malicious\r\n" +
            "--" + boundary + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.request()
            .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
            .withBody(body);

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBodyAsString().contains("Upload rejected"));
    }

    @Test
    void handleSkipsNonFileParts() throws Exception {
        String saveTo = tempDir.toString();
        String boundary = "----TestBoundary";

        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", saveTo);
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);

        String bodyStr = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"username\"\r\n" +
            "\r\n" +
            "john\r\n" +
            "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"real.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "real content\r\n" +
            "--" + boundary + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.request()
            .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
            .withBody(body);

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBodyAsString().contains("Uploaded 1"));
    }

    @Test
    void handleRespectsFileFieldsAllowList() throws Exception {
        String saveTo = tempDir.toString();
        String boundary = "----TestBoundary";

        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", saveTo);
        fileUploads.set("fileFields", objectMapper.createArrayNode().add("allowed_field"));
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);

        String bodyStr = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"denied\"; filename=\"secret.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "not allowed\r\n" +
            "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"allowed_field\"; filename=\"ok.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "allowed content\r\n" +
            "--" + boundary + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.request()
            .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
            .withBody(body);

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBodyAsString().contains("Uploaded 1"));
    }

    @Test
    void handleReturnsConfiguredHttpResponseWhenPresent() throws Exception {
        String saveTo = tempDir.toString();
        String boundary = "----TestBoundary";

        ObjectNode httpResponse = objectMapper.createObjectNode();
        httpResponse.put("statusCode", 201);
        httpResponse.put("body", "Custom response");

        ObjectNode fileUploads = objectMapper.createObjectNode();
        fileUploads.put("saveTo", saveTo);
        EnhancedExpectation config = new EnhancedExpectation();
        config.setFileUploads(fileUploads);
        config.setHttpResponse(httpResponse);

        String bodyStr = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "content\r\n" +
            "--" + boundary + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.request()
            .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
            .withBody(body);

        HttpResponse response = strategy.handle(request, config, Map.of("pathPattern", "/upload"));

        assertEquals(201, response.getStatusCode());
        assertEquals("Custom response", response.getBodyAsString());
    }
}
