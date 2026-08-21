package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for mutual TLS (mTLS) authentication.
 * <p>
 * Defines the CA certificate used to validate client certificates and whether
 * client authentication is required or optional. When enabled, clients must
 * present valid certificates signed by the configured CA to establish a connection.
 * </p>
 */
public class MtlsConfig {
    /**
     * PEM-encoded CA certificate content used to validate client certificates
     */
    @NotBlank(message = "CA certificate is required for mTLS")
    @JsonProperty("caCertificate")
    private String caCertificate;
    /**
     * Whether client certificate authentication is required (true) or optional (false). Defaults to true.
     */
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

    /**
     * PEM-encoded CA certificate content used to validate client certificates
     */
    public String getCaCertificate() {
        return this.caCertificate;
    }

    /**
     * Whether client certificate authentication is required (true) or optional (false). Defaults to true.
     */
    public boolean isRequireClientAuth() {
        return this.requireClientAuth;
    }

    /**
     * PEM-encoded CA certificate content used to validate client certificates
     */
    public void setCaCertificate(final String caCertificate) {
        this.caCertificate = caCertificate;
    }

    /**
     * Whether client certificate authentication is required (true) or optional (false). Defaults to true.
     */
    public void setRequireClientAuth(final boolean requireClientAuth) {
        this.requireClientAuth = requireClientAuth;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MtlsConfig)) return false;
        final MtlsConfig other = (MtlsConfig) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isRequireClientAuth() != other.isRequireClientAuth()) return false;
        final Object this$caCertificate = this.getCaCertificate();
        final Object other$caCertificate = other.getCaCertificate();
        if (this$caCertificate == null ? other$caCertificate != null : !this$caCertificate.equals(other$caCertificate)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MtlsConfig;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isRequireClientAuth() ? 79 : 97);
        final Object $caCertificate = this.getCaCertificate();
        result = result * PRIME + ($caCertificate == null ? 43 : $caCertificate.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "MtlsConfig(caCertificate=" + this.getCaCertificate() + ", requireClientAuth=" + this.isRequireClientAuth() + ")";
    }

    public MtlsConfig() {
    }

    /**
     * Creates a new {@code MtlsConfig} instance.
     *
     * @param caCertificate PEM-encoded CA certificate content used to validate client certificates
     * @param requireClientAuth Whether client certificate authentication is required (true) or optional (false). Defaults to true.
     */
    public MtlsConfig(final String caCertificate, final boolean requireClientAuth) {
        this.caCertificate = caCertificate;
        this.requireClientAuth = requireClientAuth;
    }
}
