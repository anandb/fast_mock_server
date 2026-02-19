package io.github.anandb.mockserver.model;

import io.github.anandb.mockserver.model.BasicAuthConfig;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.model.TlsConfig;
import org.mockserver.integration.ClientAndServer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Record representing an active mock server instance.
 */
public record ServerInstance(
    String serverId,
    int port,
    ClientAndServer server,
    TlsConfig tlsConfig,
    List<GlobalHeader> globalHeaders,
    BasicAuthConfig basicAuthConfig,
    RelayConfig relayConfig,
    LocalDateTime createdAt,
    String description
) {
    /**
     * Gets the protocol being used by this server.
     *
     * @return "https" if TLS is configured and valid, "http" otherwise
     */
    public String getProtocol() {
        return (tlsConfig != null && tlsConfig.isValid()) ? "https" : "http";
    }

    /**
     * Gets the complete base URL for accessing this server.
     *
     * @return formatted base URL (e.g., "https://localhost:1443")
     */
    public String getBaseUrl() {
        return String.format("%s://localhost:%d", getProtocol(), port);
    }

    /**
     * Checks if TLS is enabled for this server.
     *
     * @return true if TLS is configured and valid, false otherwise
     */
    public boolean isTlsEnabled() {
        return tlsConfig != null && tlsConfig.isValid();
    }

    /**
     * Checks if mutual TLS (mTLS) is enabled for this server.
     *
     * @return true if mTLS is configured and valid, false otherwise
     */
    public boolean isMtlsEnabled() {
        return tlsConfig != null && tlsConfig.hasMtls();
    }

    /**
     * Checks if basic authentication is enabled for this server.
     *
     * @return true if basic auth is configured and valid, false otherwise
     */
    public boolean isBasicAuthEnabled() {
        return basicAuthConfig != null && basicAuthConfig.isValid();
    }

    /**
     * Checks if relay configuration is enabled for this server.
     *
     * @return true if relay is configured and valid, false otherwise
     */
    public boolean isRelayEnabled() {
        return relayConfig != null && relayConfig.isValid();
    }

    /**
     * Checks if the underlying server is running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
