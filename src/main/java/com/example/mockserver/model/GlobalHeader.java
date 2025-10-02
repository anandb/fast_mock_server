package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a global header to be added to all responses from a mock server
 */
@Data
@NoArgsConstructor
public class GlobalHeader {

    @NotBlank(message = "Header name is required")
    private String name;

    @NotBlank(message = "Header value is required")
    private String value;

    @JsonCreator
    public GlobalHeader(
        @JsonProperty("name") String name,
        @JsonProperty("value") String value
    ) {
        this.name = name;
        this.value = value;
    }
}
