package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * TLS configuration for a mock server.
 * <p>
 * Accepts certificate and private key as inline PEM-encoded content. Supports optional
 * mutual TLS (mTLS) configuration for client certificate authentication.
 * </p>
 */
public class TlsConfig {
    /**
     * PEM-encoded server certificate content
     */
    @NotBlank(message = "Server certificate is required")
    @JsonProperty("certificate")
    private String certificate;
    /**
     * PEM-encoded private key content corresponding to the certificate
     */
    @NotBlank(message = "Private key is required")
    @JsonProperty("privateKey")
    private String privateKey;
    /**
     * Optional mutual TLS (mTLS) configuration for client authentication
     */
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
        return certificate != null && !certificate.trim().isEmpty() && privateKey != null && !privateKey.trim().isEmpty();
    }

    /**
     * Checks if mutual TLS (mTLS) is configured.
     *
     * @return true if mTLS configuration is present and valid, false otherwise
     */
    public boolean hasMtls() {
        return mtlsConfig != null && mtlsConfig.isValid();
    }

    public void stripSpaces() {
        certificate = certificate.replaceAll("\\s+", "");
        privateKey = privateKey.replaceAll("\\s+", "");
        if (hasMtls()) {
            mtlsConfig.setCaCertificate(mtlsConfig.getCaCertificate().replaceAll("\\s+", ""));
        }
    }

    /**
     * PEM-encoded server certificate content
     */
    public String getCertificate() {
        return this.certificate;
    }

    /**
     * PEM-encoded private key content corresponding to the certificate
     */
    public String getPrivateKey() {
        return this.privateKey;
    }

    /**
     * Optional mutual TLS (mTLS) configuration for client authentication
     */
    public MtlsConfig getMtlsConfig() {
        return this.mtlsConfig;
    }

    /**
     * PEM-encoded server certificate content
     */
    public void setCertificate(final String certificate) {
        this.certificate = certificate;
    }

    /**
     * PEM-encoded private key content corresponding to the certificate
     */
    public void setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * Optional mutual TLS (mTLS) configuration for client authentication
     */
    public void setMtlsConfig(final MtlsConfig mtlsConfig) {
        this.mtlsConfig = mtlsConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TlsConfig)) return false;
        final TlsConfig other = (TlsConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$certificate = this.getCertificate();
        final Object other$certificate = other.getCertificate();
        if (this$certificate == null ? other$certificate != null : !this$certificate.equals(other$certificate)) return false;
        final Object this$privateKey = this.getPrivateKey();
        final Object other$privateKey = other.getPrivateKey();
        if (this$privateKey == null ? other$privateKey != null : !this$privateKey.equals(other$privateKey)) return false;
        final Object this$mtlsConfig = this.getMtlsConfig();
        final Object other$mtlsConfig = other.getMtlsConfig();
        if (this$mtlsConfig == null ? other$mtlsConfig != null : !this$mtlsConfig.equals(other$mtlsConfig)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TlsConfig;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $certificate = this.getCertificate();
        result = result * PRIME + ($certificate == null ? 43 : $certificate.hashCode());
        final Object $privateKey = this.getPrivateKey();
        result = result * PRIME + ($privateKey == null ? 43 : $privateKey.hashCode());
        final Object $mtlsConfig = this.getMtlsConfig();
        result = result * PRIME + ($mtlsConfig == null ? 43 : $mtlsConfig.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "TlsConfig(certificate=" + this.getCertificate() + ", privateKey=" + this.getPrivateKey() + ", mtlsConfig=" + this.getMtlsConfig() + ")";
    }

    public TlsConfig() {
    }

    /**
     * Creates a new {@code TlsConfig} instance.
     *
     * @param certificate PEM-encoded server certificate content
     * @param privateKey PEM-encoded private key content corresponding to the certificate
     * @param mtlsConfig Optional mutual TLS (mTLS) configuration for client authentication
     */
    public TlsConfig(final String certificate, final String privateKey, final MtlsConfig mtlsConfig) {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.mtlsConfig = mtlsConfig;
    }
}
