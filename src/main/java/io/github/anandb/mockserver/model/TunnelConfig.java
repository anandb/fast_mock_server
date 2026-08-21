package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TunnelConfig {
    @NotBlank(message = "Namespace is required for tunnel configuration")
    @JsonProperty("namespace")
    private String namespace;
    @NotBlank(message = "Pod prefix is required for tunnel configuration")
    @JsonProperty("podPrefix")
    private String podPrefix;
    @NotNull(message = "Pod port is required for tunnel configuration")
    @JsonProperty("podPort")
    private Integer podPort;
    /**
     * Max time in ms to wait for tunnel to become ready before failing. Default: 10s.
     */
    @JsonProperty("tunnelReadyTimeoutMs")
    private long tunnelReadyTimeoutMs = 10000;

    /**
     * 3-arg constructor for backward compatibility (uses default timeout).
     */
    public TunnelConfig(String namespace, String podPrefix, Integer podPort) {
        this.namespace = namespace;
        this.podPrefix = podPrefix;
        this.podPort = podPort;
        this.tunnelReadyTimeoutMs = 10000;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getPodPrefix() {
        return this.podPrefix;
    }

    public Integer getPodPort() {
        return this.podPort;
    }

    /**
     * Max time in ms to wait for tunnel to become ready before failing. Default: 10s.
     */
    public long getTunnelReadyTimeoutMs() {
        return this.tunnelReadyTimeoutMs;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    public void setPodPrefix(final String podPrefix) {
        this.podPrefix = podPrefix;
    }

    public void setPodPort(final Integer podPort) {
        this.podPort = podPort;
    }

    /**
     * Max time in ms to wait for tunnel to become ready before failing. Default: 10s.
     */
    public void setTunnelReadyTimeoutMs(final long tunnelReadyTimeoutMs) {
        this.tunnelReadyTimeoutMs = tunnelReadyTimeoutMs;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TunnelConfig)) return false;
        final TunnelConfig other = (TunnelConfig) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getTunnelReadyTimeoutMs() != other.getTunnelReadyTimeoutMs()) return false;
        final Object this$podPort = this.getPodPort();
        final Object other$podPort = other.getPodPort();
        if (this$podPort == null ? other$podPort != null : !this$podPort.equals(other$podPort)) return false;
        final Object this$namespace = this.getNamespace();
        final Object other$namespace = other.getNamespace();
        if (this$namespace == null ? other$namespace != null : !this$namespace.equals(other$namespace)) return false;
        final Object this$podPrefix = this.getPodPrefix();
        final Object other$podPrefix = other.getPodPrefix();
        if (this$podPrefix == null ? other$podPrefix != null : !this$podPrefix.equals(other$podPrefix)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TunnelConfig;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $tunnelReadyTimeoutMs = this.getTunnelReadyTimeoutMs();
        result = result * PRIME + (int) ($tunnelReadyTimeoutMs >>> 32 ^ $tunnelReadyTimeoutMs);
        final Object $podPort = this.getPodPort();
        result = result * PRIME + ($podPort == null ? 43 : $podPort.hashCode());
        final Object $namespace = this.getNamespace();
        result = result * PRIME + ($namespace == null ? 43 : $namespace.hashCode());
        final Object $podPrefix = this.getPodPrefix();
        result = result * PRIME + ($podPrefix == null ? 43 : $podPrefix.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "TunnelConfig(namespace=" + this.getNamespace() + ", podPrefix=" + this.getPodPrefix() + ", podPort=" + this.getPodPort() + ", tunnelReadyTimeoutMs=" + this.getTunnelReadyTimeoutMs() + ")";
    }

    public TunnelConfig() {
    }

    /**
     * Creates a new {@code TunnelConfig} instance.
     *
     * @param namespace
     * @param podPrefix
     * @param podPort
     * @param tunnelReadyTimeoutMs Max time in ms to wait for tunnel to become ready before failing. Default: 10s.
     */
    public TunnelConfig(final String namespace, final String podPrefix, final Integer podPort, final long tunnelReadyTimeoutMs) {
        this.namespace = namespace;
        this.podPrefix = podPrefix;
        this.podPort = podPort;
        this.tunnelReadyTimeoutMs = tunnelReadyTimeoutMs;
    }
}
