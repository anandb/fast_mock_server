package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.serialization.HttpRequestSerializer;
import org.mockserver.serialization.HttpResponseSerializer;
import org.mockserver.logging.MockServerLogger;

import java.util.List;

/**
 * Data Transfer Object for enhanced expectations.
 */
public class EnhancedExpectationDTO {

    private JsonNode httpRequest;
    private JsonNode httpResponse;
    private String file;
    private Boolean sse;
    private List<String> messages;
    private Integer interval;
    private RelayConfig relay;

    public EnhancedExpectationDTO() {}

    public EnhancedExpectationDTO(JsonNode httpRequest, JsonNode httpResponse, String file, Boolean sse, List<String> messages, Integer interval, RelayConfig relay) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.file = file;
        this.sse = sse;
        this.messages = messages;
        this.interval = interval;
        this.relay = relay;
    }

    public JsonNode getHttpRequestNode() { return httpRequest; }
    public void setHttpRequestNode(JsonNode httpRequest) { this.httpRequest = httpRequest; }
    public JsonNode getHttpResponseNode() { return httpResponse; }
    public void setHttpResponseNode(JsonNode httpResponse) { this.httpResponse = httpResponse; }
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public Boolean getSse() { return sse; }
    public void setSse(Boolean sse) { this.sse = sse; }
    public List<String> getMessages() { return messages; }
    public void setMessages(List<String> messages) { this.messages = messages; }
    public Integer getInterval() { return interval; }
    public void setInterval(Integer interval) { this.interval = interval; }
    public RelayConfig getRelay() { return relay; }
    public void setRelay(RelayConfig relay) { this.relay = relay; }

    private static final HttpRequestSerializer REQUEST_SERIALIZER = new HttpRequestSerializer(new MockServerLogger());
    private static final HttpResponseSerializer RESPONSE_SERIALIZER = new HttpResponseSerializer(new MockServerLogger());

    public HttpRequest getHttpRequest() {
        return httpRequest != null ? REQUEST_SERIALIZER.deserialize(httpRequest.toString()) : null;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse != null ? RESPONSE_SERIALIZER.deserialize(httpResponse.toString()) : null;
    }

    public boolean isSse() {
        return Boolean.TRUE.equals(sse) && messages != null && !messages.isEmpty();
    }

    public boolean isFileResponse() {
        return file != null && !file.isEmpty();
    }

    public boolean isRelay() {
        return relay != null && relay.isValid();
    }

    public static EnhancedExpectationDTOBuilder builder() {
        return new EnhancedExpectationDTOBuilder();
    }

    public static class EnhancedExpectationDTOBuilder {
        private JsonNode httpRequest;
        private JsonNode httpResponse;
        private String file;
        private Boolean sse;
        private List<String> messages;
        private Integer interval;
        private RelayConfig relay;

        public EnhancedExpectationDTOBuilder httpRequest(JsonNode httpRequest) { this.httpRequest = httpRequest; return this; }
        public EnhancedExpectationDTOBuilder httpResponse(JsonNode httpResponse) { this.httpResponse = httpResponse; return this; }
        public EnhancedExpectationDTOBuilder file(String file) { this.file = file; return this; }
        public EnhancedExpectationDTOBuilder sse(Boolean sse) { this.sse = sse; return this; }
        public EnhancedExpectationDTOBuilder messages(List<String> messages) { this.messages = messages; return this; }
        public EnhancedExpectationDTOBuilder interval(Integer interval) { this.interval = interval; return this; }
        public EnhancedExpectationDTOBuilder relay(RelayConfig relay) { this.relay = relay; return this; }

        public EnhancedExpectationDTO build() {
            return new EnhancedExpectationDTO(httpRequest, httpResponse, file, sse, messages, interval, relay);
        }
    }
}
