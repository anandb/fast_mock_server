package io.github.anandb.mockserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

/**
 * DTO representing the full response received from an external service.
 */
@Data
@AllArgsConstructor
public class HttpResponseDTO {
    private String status; // e.g., "OK", "ERROR"
    private String body;   // JSON string or text content
    private Map<String, String> headers; // Response headers
}