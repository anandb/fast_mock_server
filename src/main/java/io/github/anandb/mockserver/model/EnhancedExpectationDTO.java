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

import java.util.List;

/**
 * Data Transfer Object for enhanced expectations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedExpectationDTO {

    private JsonNode httpRequest;
    private JsonNode httpResponse;
    private Boolean sse;
    private Integer interval;

    private static final HttpRequestSerializer REQUEST_SERIALIZER = new HttpRequestSerializer(new MockServerLogger());
    private static final HttpResponseSerializer RESPONSE_SERIALIZER = new HttpResponseSerializer(new MockServerLogger());

    public HttpRequest getHttpRequest() {
        return httpRequest != null ? REQUEST_SERIALIZER.deserialize(httpRequest.toString()) : null;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse != null ? RESPONSE_SERIALIZER.deserialize(httpResponse.toString()) : null;
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

    public static EnhancedExpectationDTOBuilder builder() {
        return new EnhancedExpectationDTOBuilder();
    }

    public static class EnhancedExpectationDTOBuilder {
        private JsonNode httpRequest;
        private JsonNode httpResponse;
        private Boolean sse;
        private Integer interval;

        public EnhancedExpectationDTOBuilder httpRequest(JsonNode httpRequest) { this.httpRequest = httpRequest; return this; }
        public EnhancedExpectationDTOBuilder httpResponse(JsonNode httpResponse) { this.httpResponse = httpResponse; return this; }
        public EnhancedExpectationDTOBuilder sse(Boolean sse) { this.sse = sse; return this; }
        public EnhancedExpectationDTOBuilder interval(Integer interval) { this.interval = interval; return this; }

        public EnhancedExpectationDTO build() {
            return new EnhancedExpectationDTO(httpRequest, httpResponse, sse, interval);
        }
    }
}
