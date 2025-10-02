package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for mutual TLS (mTLS) authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtlsConfig {

    @NotBlank(message = "CA certificate is required for mTLS")
    @JsonProperty("caCertificate")
    private String caCertificate;  // PEM-encoded CA certificate content

    @JsonProperty("requireClientAuth")
    private boolean requireClientAuth = true;  // Default to required

    /**
     * Validate that the configuration is valid
     */
    public boolean isValid() {
        return caCertificate != null && !caCertificate.trim().isEmpty();
    }
}
