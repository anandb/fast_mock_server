package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * TLS configuration for a mock server.
 * <p>
 * Accepts certificate and private key as inline PEM-encoded content. Supports optional
 * mutual TLS (mTLS) configuration for client certificate authentication.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TlsConfig {

    /** PEM-encoded server certificate content */
    @NotBlank(message = "Server certificate is required")
    @JsonProperty("certificate")
    private String certificate;

    /** PEM-encoded private key content corresponding to the certificate */
    @NotBlank(message = "Private key is required")
    @JsonProperty("privateKey")
    private String privateKey;

    /** Optional mutual TLS (mTLS) configuration for client authentication */
    @Valid
    @JsonProperty("mtlsConfig")
    private MtlsConfig mtlsConfig;

    /**
     * Checks if the TLS configuration is valid.
     * <p>
     * A configuration is considered valid if both certificate and private key are provided
     * and non-empty.
     * </p>
     *
     * @return true if both certificate and private key are present and non-empty, false otherwise
     */
    public boolean isValid() {
        return certificate != null && !certificate.trim().isEmpty() &&
               privateKey != null && !privateKey.trim().isEmpty();
    }

    /**
     * Checks if mutual TLS (mTLS) is configured.
     *
     * @return true if mTLS configuration is present and valid, false otherwise
     */
    public boolean hasMtls() {
        return mtlsConfig != null && mtlsConfig.isValid();
    }
}
