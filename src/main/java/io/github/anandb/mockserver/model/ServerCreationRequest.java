package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request object for creating a new mock server instance.
 * <p>
 * Contains all configuration parameters needed to create and configure a mock server,
 * including server identification, network settings, TLS configuration, and global headers.
 * </p>
 */
public class ServerCreationRequest {

    /** Unique identifier for the server instance */
    @NotBlank(message = "Server ID is required")
    @JsonProperty("serverId")
    private String serverId;

    /** Port number the server will listen on (must be between 1024 and 65535) */
    @NotNull(message = "Port is required")
    @Min(value = 1024, message = "Port must be between 1024 and 65535")
    @Max(value = 65535, message = "Port must be between 1024 and 65535")
    @JsonProperty("port")
    private Integer port;

    /** Optional human-readable description of the server's purpose */
    @JsonProperty("description")
    private String description;

    /** Optional TLS/mTLS configuration for secure connections */
    @Valid
    @JsonProperty("tlsConfig")
    private TlsConfig tlsConfig;

    /** Optional list of global headers to be added to all responses */
    @JsonProperty("globalHeaders")
    private List<GlobalHeader> globalHeaders;

    /** Optional basic authentication configuration */
    @Valid
    @JsonProperty("basicAuthConfig")
    private BasicAuthConfig basicAuthConfig;

    /** Optional relay configuration for forwarding requests to a remote server */
    @Valid
    @JsonProperty("relayConfig")
    private RelayConfig relayConfig;

    public ServerCreationRequest() {}

    public ServerCreationRequest(String serverId, Integer port, String description, TlsConfig tlsConfig, List<GlobalHeader> globalHeaders, BasicAuthConfig basicAuthConfig, RelayConfig relayConfig) {
        this.serverId = serverId;
        this.port = port;
        this.description = description;
        this.tlsConfig = tlsConfig;
        this.globalHeaders = globalHeaders;
        this.basicAuthConfig = basicAuthConfig;
        this.relayConfig = relayConfig;
    }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TlsConfig getTlsConfig() { return tlsConfig; }
    public void setTlsConfig(TlsConfig tlsConfig) { this.tlsConfig = tlsConfig; }
    public List<GlobalHeader> getGlobalHeaders() { return globalHeaders; }
    public void setGlobalHeaders(List<GlobalHeader> globalHeaders) { this.globalHeaders = globalHeaders; }
    public BasicAuthConfig getBasicAuthConfig() { return basicAuthConfig; }
    public void setBasicAuthConfig(BasicAuthConfig basicAuthConfig) { this.basicAuthConfig = basicAuthConfig; }
    public RelayConfig getRelayConfig() { return relayConfig; }
    public void setRelayConfig(RelayConfig relayConfig) { this.relayConfig = relayConfig; }

    /**
     * Checks if TLS is enabled and properly configured for this server.
     *
     * @return true if TLS configuration is present and valid, false otherwise
     */
    public boolean isTlsEnabled() {
        return tlsConfig != null && tlsConfig.isValid();
    }

    /**
     * Checks if basic authentication is enabled and properly configured for this server.
     *
     * @return true if basic auth configuration is present and valid, false otherwise
     */
    public boolean isBasicAuthEnabled() {
        return basicAuthConfig != null && basicAuthConfig.isValid();
    }

    /**
     * Checks if global headers are configured for this server.
     *
     * @return true if at least one global header is configured, false otherwise
     */
    public boolean hasGlobalHeaders() {
        return globalHeaders != null && !globalHeaders.isEmpty();
    }

    /**
     * Checks if relay configuration is enabled and properly configured for this server.
     *
     * @return true if relay configuration is present and valid, false otherwise
     */
    public boolean isRelayEnabled() {
        return relayConfig != null && relayConfig.isValid();
    }
}
