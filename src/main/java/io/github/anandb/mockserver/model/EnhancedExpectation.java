package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.serialization.HttpRequestSerializer;
import org.mockserver.serialization.HttpResponseSerializer;
import org.mockserver.logging.MockServerLogger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/**
 * Data Transfer Object for enhanced expectations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedExpectation {

    private JsonNode httpRequest;
    private JsonNode httpResponse;
    private JsonNode fileUploads;
    private Boolean sse;
    private Integer interval;

    private static final HttpRequestSerializer REQUEST_SERIALIZER = new HttpRequestSerializer(new MockServerLogger());
    private static final HttpResponseSerializer RESPONSE_SERIALIZER = new HttpResponseSerializer(new MockServerLogger());

    public HttpRequest getHttpRequest() {
        return httpRequest != null ? REQUEST_SERIALIZER.deserialize(httpRequest.toString()) : null;
    }

    public HttpResponse getHttpResponse() {
        if (httpResponse == null) {
            return null;
        }
        // Remove custom fields not recognized by MockServer's HttpRequestSerializer
        // before passing to the serializer to avoid schema validation errors.
        if (httpResponse instanceof ObjectNode objectNode) {
            ObjectNode cleaned = objectNode.deepCopy();
            cleaned.remove("file");
            cleaned.remove("messages");
            cleaned.remove("fileDisposition");
            return RESPONSE_SERIALIZER.deserialize(cleaned.toString());
        }
        return RESPONSE_SERIALIZER.deserialize(httpResponse.toString());
    }

    public boolean isSse() {
        List<String> messages = getMessages();
        return Boolean.TRUE.equals(sse) && messages != null && !messages.isEmpty();
    }

    public List<String> getMessages() {
        if (httpResponse == null || !httpResponse.has("messages")) {
            return List.of();
        }
        JsonNode messagesNode = httpResponse.get("messages");
        if (messagesNode.isArray()) {
            java.util.List<String> messageList = new java.util.ArrayList<>();
            messagesNode.forEach(node -> messageList.add(node.asText()));
            return messageList;
        }
        return List.of();
    }

    public boolean isFileResponse() {
        String file = getFile();
        return file != null && !file.isEmpty();
    }

    public String getFile() {
        if (httpResponse == null || !httpResponse.has("file")) {
            return null;
        }
        JsonNode fileNode = httpResponse.get("file");
        return fileNode.asText();
    }

    /**
     * Returns the file disposition mode: "inline" to serve content directly,
     * "attachment" (or absent) to trigger a download.
     */
    public String getFileDisposition() {
        if (httpResponse == null || !httpResponse.has("fileDisposition")) {
            return "attachment";
        }
        JsonNode node = httpResponse.get("fileDisposition");
        String value = node.asText();
        return value != null && !value.isEmpty() ? value : "attachment";
    }

    public boolean hasFileUploads() {
        return fileUploads != null && !fileUploads.isNull();
    }

    public FileUploadConfig getFileUploadsConfig() {
        if (fileUploads == null || fileUploads.isNull()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().treeToValue(fileUploads, FileUploadConfig.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static EnhancedExpectationDTOBuilder builder() {
        return new EnhancedExpectationDTOBuilder();
    }

    public static class EnhancedExpectationDTOBuilder {
        private JsonNode httpRequest;
        private JsonNode httpResponse;
        private JsonNode fileUploads;
        private Boolean sse;
        private Integer interval;

        public EnhancedExpectationDTOBuilder httpRequest(JsonNode httpRequest) { this.httpRequest = httpRequest; return this; }
        public EnhancedExpectationDTOBuilder httpResponse(JsonNode httpResponse) { this.httpResponse = httpResponse; return this; }
        public EnhancedExpectationDTOBuilder fileUploads(JsonNode fileUploads) { this.fileUploads = fileUploads; return this; }
        public EnhancedExpectationDTOBuilder sse(Boolean sse) { this.sse = sse; return this; }
        public EnhancedExpectationDTOBuilder interval(Integer interval) { this.interval = interval; return this; }

        public EnhancedExpectation build() {
            return new EnhancedExpectation(httpRequest, httpResponse, fileUploads, sse, interval);
        }
    }
}
