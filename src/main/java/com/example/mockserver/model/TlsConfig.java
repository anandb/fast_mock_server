package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * TLS configuration for a mock server
 * Accepts certificate and private key as inline PEM-encoded content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TlsConfig {

    @NotBlank(message = "Server certificate is required")
    @JsonProperty("certificate")
    private String certificate;  // PEM-encoded certificate content

    @NotBlank(message = "Private key is required")
    @JsonProperty("privateKey")
    private String privateKey;  // PEM-encoded private key content

    @Valid
    @JsonProperty("mtlsConfig")
    private MtlsConfig mtlsConfig;  // Optional mTLS configuration

    /**
     * Check if TLS configuration is valid
     */
    public boolean isValid() {
        return certificate != null && !certificate.trim().isEmpty() &&
               privateKey != null && !privateKey.trim().isEmpty();
    }

    /**
     * Check if mTLS is configured
     */
    public boolean hasMtls() {
        return mtlsConfig != null && mtlsConfig.isValid();
    }
}
