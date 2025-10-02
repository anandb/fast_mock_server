package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request object for creating a new mock server
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateServerRequest {

    @NotBlank(message = "Server ID is required")
    @JsonProperty("serverId")
    private String serverId;

    @NotNull(message = "Port is required")
    @Min(value = 1024, message = "Port must be between 1024 and 65535")
    @Max(value = 65535, message = "Port must be between 1024 and 65535")
    @JsonProperty("port")
    private Integer port;

    @JsonProperty("description")
    private String description;

    @Valid
    @JsonProperty("tlsConfig")
    private TlsConfig tlsConfig;  // Optional TLS configuration

    @JsonProperty("globalHeaders")
    private List<GlobalHeader> globalHeaders;  // Optional global headers

    /**
     * Check if TLS is enabled
     */
    public boolean isTlsEnabled() {
        return tlsConfig != null && tlsConfig.isValid();
    }

    /**
     * Check if global headers are configured
     */
    public boolean hasGlobalHeaders() {
        return globalHeaders != null && !globalHeaders.isEmpty();
    }
}
