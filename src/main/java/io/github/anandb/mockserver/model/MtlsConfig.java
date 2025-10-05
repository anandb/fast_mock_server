package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for mutual TLS (mTLS) authentication.
 * <p>
 * Defines the CA certificate used to validate client certificates and whether
 * client authentication is required or optional. When enabled, clients must
 * present valid certificates signed by the configured CA to establish a connection.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MtlsConfig {

    /** PEM-encoded CA certificate content used to validate client certificates */
    @NotBlank(message = "CA certificate is required for mTLS")
    @JsonProperty("caCertificate")
    private String caCertificate;

    /** Whether client certificate authentication is required (true) or optional (false). Defaults to true. */
    @JsonProperty("requireClientAuth")
    private boolean requireClientAuth = true;

    /**
     * Validates that the mTLS configuration is valid.
     * <p>
     * A configuration is considered valid if the CA certificate is provided and non-empty.
     * </p>
     *
     * @return true if the CA certificate is present and non-empty, false otherwise
     */
    public boolean isValid() {
        return caCertificate != null && !caCertificate.trim().isEmpty();
    }
}
