package io.github.anandb.mockserver.model;

import lombok.Data;

/**
 * DTO representing an incoming request structure used for expectations/requests.
 */
@Data
public class MockRequestDTO {
    private String method; // e.g., GET, POST
    private String path;   // The endpoint path, possibly with placeholders like /users/{id}
    private Object body;   // Request payload/body (nullable)

    /**
     * Constructor for basic request details (method and path).
     * @param method HTTP method.
     * @param path Endpoint path.
     */
    public MockRequestDTO(String method, String path) {
        this.method = method;
        this.path = path;
    }
}