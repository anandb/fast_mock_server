package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a global header to be added to all responses from a mock server.
 * <p>
 * Global headers are automatically merged with response-specific headers for all expectations.
 * If a specific expectation defines a header with the same name, the expectation's header
 * takes precedence over the global header.
 * </p>
 */
@Data
@NoArgsConstructor
public class GlobalHeader {

    /** The name of the header (e.g., "Content-Type", "X-Custom-Header") */
    @NotBlank(message = "Header name is required")
    private String name;

    /** The value of the header */
    @NotBlank(message = "Header value is required")
    private String value;

    /**
     * Constructs a new GlobalHeader with the specified name and value.
     *
     * @param name the header name
     * @param value the header value
     */
    @JsonCreator
    public GlobalHeader(
        @JsonProperty("name") String name,
        @JsonProperty("value") String value
    ) {
        this.name = name;
        this.value = value;
    }
}
