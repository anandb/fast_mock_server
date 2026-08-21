package io.github.anandb.mockserver.model;

/**
 * DTO representing an incoming request structure used for expectations/requests.
 */
public class MockRequestDTO {
    private String method; // e.g., GET, POST
    private String path; // The endpoint path, possibly with placeholders like /users/{id}
    private Object body; // Request payload/body (nullable)

    /**
     * Constructor for basic request details (method and path).
     * @param method HTTP method.
     * @param path Endpoint path.
     */
    public MockRequestDTO(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public String getMethod() {
        return this.method;
    }

    public String getPath() {
        return this.path;
    }

    public Object getBody() {
        return this.body;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setBody(final Object body) {
        this.body = body;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MockRequestDTO)) return false;
        final MockRequestDTO other = (MockRequestDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$method = this.getMethod();
        final Object other$method = other.getMethod();
        if (this$method == null ? other$method != null : !this$method.equals(other$method)) return false;
        final Object this$path = this.getPath();
        final Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        final Object this$body = this.getBody();
        final Object other$body = other.getBody();
        if (this$body == null ? other$body != null : !this$body.equals(other$body)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MockRequestDTO;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $method = this.getMethod();
        result = result * PRIME + ($method == null ? 43 : $method.hashCode());
        final Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final Object $body = this.getBody();
        result = result * PRIME + ($body == null ? 43 : $body.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "MockRequestDTO(method=" + this.getMethod() + ", path=" + this.getPath() + ", body=" + this.getBody() + ")";
    }

    public MockRequestDTO() {
    }
}
