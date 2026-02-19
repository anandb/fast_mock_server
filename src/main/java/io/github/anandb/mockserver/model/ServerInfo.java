package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response object containing information about a mock server instance.
 * <p>
 * Provides comprehensive details about a server's configuration, status, and runtime information.
 * Used in API responses to inform clients about server state and capabilities.
 * </p>
 */
public class ServerInfo {

    /** Unique identifier of the server instance */
    @JsonProperty("serverId")
    private String serverId;

    /** Port number the server is listening on */
    @JsonProperty("port")
    private int port;

    /** Human-readable description of the server's purpose */
    @JsonProperty("description")
    private String description;

    /** Protocol being used: "http" or "https" */
    @JsonProperty("protocol")
    private String protocol;

    /** Complete base URL for accessing the server (e.g., "https://localhost:1443") */
    @JsonProperty("baseUrl")
    private String baseUrl;

    /** Indicates whether TLS is enabled for this server */
    @JsonProperty("tlsEnabled")
    private boolean tlsEnabled;

    /** Indicates whether mutual TLS (mTLS) is enabled for this server */
    @JsonProperty("mtlsEnabled")
    private boolean mtlsEnabled;

    /** List of global headers configured for all responses from this server */
    @JsonProperty("globalHeaders")
    private List<GlobalHeader> globalHeaders;

    /** Indicates whether basic authentication is enabled for this server */
    @JsonProperty("basicAuthEnabled")
    private boolean basicAuthEnabled;

    /** Indicates whether relay configuration is enabled for this server */
    @JsonProperty("relayEnabled")
    private boolean relayEnabled;

    /** Timestamp when the server was created */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /** Current status of the server (e.g., "running", "stopped") */
    @JsonProperty("status")
    private String status;

    public ServerInfo() {}

    public ServerInfo(String serverId, int port, String description, String protocol, String baseUrl, boolean tlsEnabled, boolean mtlsEnabled, List<GlobalHeader> globalHeaders, boolean basicAuthEnabled, boolean relayEnabled, LocalDateTime createdAt, String status) {
        this.serverId = serverId;
        this.port = port;
        this.description = description;
        this.protocol = protocol;
        this.baseUrl = baseUrl;
        this.tlsEnabled = tlsEnabled;
        this.mtlsEnabled = mtlsEnabled;
        this.globalHeaders = globalHeaders;
        this.basicAuthEnabled = basicAuthEnabled;
        this.relayEnabled = relayEnabled;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public boolean isTlsEnabled() { return tlsEnabled; }
    public void setTlsEnabled(boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; }
    public boolean isMtlsEnabled() { return mtlsEnabled; }
    public void setMtlsEnabled(boolean mtlsEnabled) { this.mtlsEnabled = mtlsEnabled; }
    public List<GlobalHeader> getGlobalHeaders() { return globalHeaders; }
    public void setGlobalHeaders(List<GlobalHeader> globalHeaders) { this.globalHeaders = globalHeaders; }
    public boolean isBasicAuthEnabled() { return basicAuthEnabled; }
    public void setBasicAuthEnabled(boolean basicAuthEnabled) { this.basicAuthEnabled = basicAuthEnabled; }
    public boolean isRelayEnabled() { return relayEnabled; }
    public void setRelayEnabled(boolean relayEnabled) { this.relayEnabled = relayEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static ServerInfoBuilder builder() {
        return new ServerInfoBuilder();
    }

    public static class ServerInfoBuilder {
        private String serverId;
        private int port;
        private String description;
        private String protocol;
        private String baseUrl;
        private boolean tlsEnabled;
        private boolean mtlsEnabled;
        private List<GlobalHeader> globalHeaders;
        private boolean basicAuthEnabled;
        private boolean relayEnabled;
        private LocalDateTime createdAt;
        private String status;

        public ServerInfoBuilder serverId(String serverId) { this.serverId = serverId; return this; }
        public ServerInfoBuilder port(int port) { this.port = port; return this; }
        public ServerInfoBuilder description(String description) { this.description = description; return this; }
        public ServerInfoBuilder protocol(String protocol) { this.protocol = protocol; return this; }
        public ServerInfoBuilder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public ServerInfoBuilder tlsEnabled(boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; return this; }
        public ServerInfoBuilder mtlsEnabled(boolean mtlsEnabled) { this.mtlsEnabled = mtlsEnabled; return this; }
        public ServerInfoBuilder globalHeaders(List<GlobalHeader> globalHeaders) { this.globalHeaders = globalHeaders; return this; }
        public ServerInfoBuilder basicAuthEnabled(boolean basicAuthEnabled) { this.basicAuthEnabled = basicAuthEnabled; return this; }
        public ServerInfoBuilder relayEnabled(boolean relayEnabled) { this.relayEnabled = relayEnabled; return this; }
        public ServerInfoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ServerInfoBuilder status(String status) { this.status = status; return this; }

        public ServerInfo build() {
            return new ServerInfo(serverId, port, description, protocol, baseUrl, tlsEnabled, mtlsEnabled, globalHeaders, basicAuthEnabled, relayEnabled, createdAt, status);
        }
    }
}
