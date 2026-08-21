package io.github.anandb.mockserver.model;

import java.util.Map;

/**
 * DTO representing the full response received from an external service.
 */
public class HttpResponseDTO {
    private String status; // e.g., "OK", "ERROR"
    private String body; // JSON string or text content
    private Map<String, String> headers; // Response headers

    public String getStatus() {
        return this.status;
    }

    public String getBody() {
        return this.body;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HttpResponseDTO)) return false;
        final HttpResponseDTO other = (HttpResponseDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$status = this.getStatus();
        final Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final Object this$body = this.getBody();
        final Object other$body = other.getBody();
        if (this$body == null ? other$body != null : !this$body.equals(other$body)) return false;
        final Object this$headers = this.getHeaders();
        final Object other$headers = other.getHeaders();
        if (this$headers == null ? other$headers != null : !this$headers.equals(other$headers)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HttpResponseDTO;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final Object $body = this.getBody();
        result = result * PRIME + ($body == null ? 43 : $body.hashCode());
        final Object $headers = this.getHeaders();
        result = result * PRIME + ($headers == null ? 43 : $headers.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "HttpResponseDTO(status=" + this.getStatus() + ", body=" + this.getBody() + ", headers=" + this.getHeaders() + ")";
    }

    public HttpResponseDTO(final String status, final String body, final Map<String, String> headers) {
        this.status = status;
        this.body = body;
        this.headers = headers;
    }
}
