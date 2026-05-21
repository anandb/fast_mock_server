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
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("DynamicFileStrategy Tests")
@ExtendWith(MockitoExtension.class)
class DynamicFileStrategyTest {

    @Mock
    private FreemarkerTemplateService templateService;

    private DynamicFileStrategy strategy;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        strategy = new DynamicFileStrategy(templateService);
        mapper = new ObjectMapper();
    }

    @Test
    void supportsReturnsTrueWhenFileResponse() {
        ObjectNode httpResponse = mapper.createObjectNode().put("file", "/data/file.pdf");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        assertTrue(strategy.supports(config));
    }

    @Test
    void supportsReturnsTrueWhenFreemarkerTemplate() {
        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200).put("body", "${template}");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        assertTrue(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseForPlainBody() {
        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200).put("body", "plain text");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        assertFalse(strategy.supports(config));
    }

    @Test
    void supportsReturnsFalseWhenHttpResponseNull() {
        EnhancedExpectation config = new EnhancedExpectation();
        assertFalse(strategy.supports(config));
    }

    @Test
    void handleTemplateResponseProcessesTemplate(@TempDir Path tempDir) throws Exception {
        ObjectNode httpResponse = mapper.createObjectNode()
                .put("statusCode", 200)
                .put("body", "Hello ${name}!");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        when(templateService.processTemplateWithRequest(anyString(), any(), any()))
                .thenReturn("Hello World!");

        HttpResponse result = strategy.handle(
                HttpRequest.request().withPath("/test"),
                config,
                Map.of("pathPattern", "/test")
        );

        assertEquals(200, result.getStatusCode());
        assertEquals("Hello World!", result.getBodyAsString());
    }

    @Test
    void handleTemplateResponseReturns500OnError() throws Exception {
        ObjectNode httpResponse = mapper.createObjectNode().put("body", "${bad}");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        when(templateService.processTemplateWithRequest(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Template error"));

        HttpResponse result = strategy.handle(
                HttpRequest.request().withPath("/test"),
                config,
                Map.of("pathPattern", "/test")
        );

        assertEquals(500, result.getStatusCode());
    }

    @Test
    void handleFileResponseServesExistingFile(@TempDir Path tempDir) throws Exception {
        Path subDir = tempDir.resolve("files");
        Files.createDirectories(subDir);
        Path file = subDir.resolve("test-file.pdf");
        Files.writeString(file, "pdf-content");

        ObjectNode httpResponse = mapper.createObjectNode()
                .put("statusCode", 200)
                .put("file", subDir.toString() + "/test-file");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        HttpResponse result = strategy.handle(
                HttpRequest.request().withPath("/test"),
                config,
                Map.of("pathPattern", "/test")
        );

        assertEquals(200, result.getStatusCode());
        assertArrayEquals("pdf-content".getBytes(), result.getBodyAsRawBytes());
    }

    @Test
    void handleFileResponseReturns404WhenFileNotFound(@TempDir Path tempDir) {
        ObjectNode httpResponse = mapper.createObjectNode()
                .put("file", tempDir.toString() + "/nonexistent-file");
        EnhancedExpectation config = EnhancedExpectation.builder()
                .httpResponse(httpResponse)
                .build();

        HttpResponse result = strategy.handle(
                HttpRequest.request().withPath("/test"),
                config,
                Map.of("pathPattern", "/test")
        );

        assertEquals(404, result.getStatusCode());
    }

    @Test
    void determineContentTypeReturnsCorrectType(@TempDir Path tempDir) throws Exception {
        Path subDir = tempDir.resolve("types");
        Files.createDirectories(subDir);

        // Create test files
        Path jsonFile = subDir.resolve("data.json");
        Files.writeString(jsonFile, "{}");
        Path xmlFile = subDir.resolve("data.xml");
        Files.writeString(xmlFile, "<root/>");
        Path txtFile = subDir.resolve("readme.txt");
        Files.writeString(txtFile, "text");
        Path jpgFile = subDir.resolve("image.jpg");
        Files.writeString(jpgFile, "data");

        ObjectNode httpResponse = mapper.createObjectNode().put("statusCode", 200);

        // Test JSON file response
        EnhancedExpectation jsonConfig = EnhancedExpectation.builder()
                .httpResponse(httpResponse.deepCopy().put("file", subDir.toString() + "/data.json"))
                .build();
        HttpResponse jsonResult = strategy.handle(
                HttpRequest.request().withPath("/test"),
                jsonConfig,
                Map.of("pathPattern", "/test")
        );
        assertEquals(200, jsonResult.getStatusCode());
    }

    @Test
    void getPriorityIs10() {
        assertEquals(10, strategy.getPriority());
    }
}
