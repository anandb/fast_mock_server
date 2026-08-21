package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.databind.JsonNode;
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

        public EnhancedExpectationDTOBuilder httpRequest(JsonNode httpRequest) {
            this.httpRequest = httpRequest;
            return this;
        }

        public EnhancedExpectationDTOBuilder httpResponse(JsonNode httpResponse) {
            this.httpResponse = httpResponse;
            return this;
        }

        public EnhancedExpectationDTOBuilder fileUploads(JsonNode fileUploads) {
            this.fileUploads = fileUploads;
            return this;
        }

        public EnhancedExpectationDTOBuilder sse(Boolean sse) {
            this.sse = sse;
            return this;
        }

        public EnhancedExpectationDTOBuilder interval(Integer interval) {
            this.interval = interval;
            return this;
        }

        public EnhancedExpectation build() {
            return new EnhancedExpectation(httpRequest, httpResponse, fileUploads, sse, interval);
        }
    }

    public JsonNode getFileUploads() {
        return this.fileUploads;
    }

    public Boolean getSse() {
        return this.sse;
    }

    public Integer getInterval() {
        return this.interval;
    }

    public void setHttpRequest(final JsonNode httpRequest) {
        this.httpRequest = httpRequest;
    }

    public void setHttpResponse(final JsonNode httpResponse) {
        this.httpResponse = httpResponse;
    }

    public void setFileUploads(final JsonNode fileUploads) {
        this.fileUploads = fileUploads;
    }

    public void setSse(final Boolean sse) {
        this.sse = sse;
    }

    public void setInterval(final Integer interval) {
        this.interval = interval;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof EnhancedExpectation)) return false;
        final EnhancedExpectation other = (EnhancedExpectation) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$sse = this.getSse();
        final Object other$sse = other.getSse();
        if (this$sse == null ? other$sse != null : !this$sse.equals(other$sse)) return false;
        final Object this$interval = this.getInterval();
        final Object other$interval = other.getInterval();
        if (this$interval == null ? other$interval != null : !this$interval.equals(other$interval)) return false;
        final Object this$httpRequest = this.getHttpRequest();
        final Object other$httpRequest = other.getHttpRequest();
        if (this$httpRequest == null ? other$httpRequest != null : !this$httpRequest.equals(other$httpRequest)) return false;
        final Object this$httpResponse = this.getHttpResponse();
        final Object other$httpResponse = other.getHttpResponse();
        if (this$httpResponse == null ? other$httpResponse != null : !this$httpResponse.equals(other$httpResponse)) return false;
        final Object this$fileUploads = this.getFileUploads();
        final Object other$fileUploads = other.getFileUploads();
        if (this$fileUploads == null ? other$fileUploads != null : !this$fileUploads.equals(other$fileUploads)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof EnhancedExpectation;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $sse = this.getSse();
        result = result * PRIME + ($sse == null ? 43 : $sse.hashCode());
        final Object $interval = this.getInterval();
        result = result * PRIME + ($interval == null ? 43 : $interval.hashCode());
        final Object $httpRequest = this.getHttpRequest();
        result = result * PRIME + ($httpRequest == null ? 43 : $httpRequest.hashCode());
        final Object $httpResponse = this.getHttpResponse();
        result = result * PRIME + ($httpResponse == null ? 43 : $httpResponse.hashCode());
        final Object $fileUploads = this.getFileUploads();
        result = result * PRIME + ($fileUploads == null ? 43 : $fileUploads.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "EnhancedExpectation(httpRequest=" + this.getHttpRequest() + ", httpResponse=" + this.getHttpResponse() + ", fileUploads=" + this.getFileUploads() + ", sse=" + this.getSse() + ", interval=" + this.getInterval() + ")";
    }

    public EnhancedExpectation() {
    }

    public EnhancedExpectation(final JsonNode httpRequest, final JsonNode httpResponse, final JsonNode fileUploads, final Boolean sse, final Integer interval) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.fileUploads = fileUploads;
        this.sse = sse;
        this.interval = interval;
    }
}
