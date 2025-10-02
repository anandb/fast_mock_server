package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Represents a complete server configuration including its creation parameters and expectations.
 * <p>
 * This model is used when loading server configurations from a JSON file at startup.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfiguration {

    /** Server creation parameters */
    @NotNull(message = "Server configuration is required")
    @Valid
    @JsonProperty("server")
    private CreateServerRequest server;

    /** Optional list of expectations to configure on this server at startup */
    @JsonProperty("expectations")
    private List<Object> expectations;

    /**
     * Checks if this server has any expectations configured.
     *
     * @return true if at least one expectation is configured, false otherwise
     */
    public boolean hasExpectations() {
        return expectations != null && !expectations.isEmpty();
    }
}
