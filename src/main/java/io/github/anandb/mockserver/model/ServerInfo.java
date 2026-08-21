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
    /**
     * Unique identifier of the server instance
     */
    @JsonProperty("serverId")
    private String serverId;
    /**
     * Port number the server is listening on
     */
    @JsonProperty("port")
    private int port;
    /**
     * Human-readable description of the server's purpose
     */
    @JsonProperty("description")
    private String description;
    /**
     * Protocol being used: "http" or "https"
     */
    @JsonProperty("protocol")
    private String protocol;
    /**
     * Complete base URL for accessing the server (e.g., "https://localhost:1443")
     */
    @JsonProperty("baseUrl")
    private String baseUrl;
    /**
     * Indicates whether TLS is enabled for this server
     */
    @JsonProperty("tlsEnabled")
    private boolean tlsEnabled;
    /**
     * Indicates whether mutual TLS (mTLS) is enabled for this server
     */
    @JsonProperty("mtlsEnabled")
    private boolean mtlsEnabled;
    /**
     * List of global headers configured for all responses from this server
     */
    @JsonProperty("globalHeaders")
    private List<GlobalHeader> globalHeaders;
    /**
     * Indicates whether basic authentication is enabled for this server
     */
    @JsonProperty("basicAuthEnabled")
    private boolean basicAuthEnabled;
    /**
     * Indicates whether relay configuration is enabled for this server
     */
    @JsonProperty("relayEnabled")
    private boolean relayEnabled;
    /**
     * Timestamp when the server was created
     */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    /**
     * Current status of the server (e.g., "running", "stopped")
     */
    @JsonProperty("status")
    private String status;


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

        ServerInfoBuilder() {
        }

        /**
         * Unique identifier of the server instance
         * @return {@code this}.
         */
        @JsonProperty("serverId")
        public ServerInfo.ServerInfoBuilder serverId(final String serverId) {
            this.serverId = serverId;
            return this;
        }

        /**
         * Port number the server is listening on
         * @return {@code this}.
         */
        @JsonProperty("port")
        public ServerInfo.ServerInfoBuilder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Human-readable description of the server's purpose
         * @return {@code this}.
         */
        @JsonProperty("description")
        public ServerInfo.ServerInfoBuilder description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * Protocol being used: "http" or "https"
         * @return {@code this}.
         */
        @JsonProperty("protocol")
        public ServerInfo.ServerInfoBuilder protocol(final String protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Complete base URL for accessing the server (e.g., "https://localhost:1443")
         * @return {@code this}.
         */
        @JsonProperty("baseUrl")
        public ServerInfo.ServerInfoBuilder baseUrl(final String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Indicates whether TLS is enabled for this server
         * @return {@code this}.
         */
        @JsonProperty("tlsEnabled")
        public ServerInfo.ServerInfoBuilder tlsEnabled(final boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
            return this;
        }

        /**
         * Indicates whether mutual TLS (mTLS) is enabled for this server
         * @return {@code this}.
         */
        @JsonProperty("mtlsEnabled")
        public ServerInfo.ServerInfoBuilder mtlsEnabled(final boolean mtlsEnabled) {
            this.mtlsEnabled = mtlsEnabled;
            return this;
        }

        /**
         * List of global headers configured for all responses from this server
         * @return {@code this}.
         */
        @JsonProperty("globalHeaders")
        public ServerInfo.ServerInfoBuilder globalHeaders(final List<GlobalHeader> globalHeaders) {
            this.globalHeaders = globalHeaders;
            return this;
        }

        /**
         * Indicates whether basic authentication is enabled for this server
         * @return {@code this}.
         */
        @JsonProperty("basicAuthEnabled")
        public ServerInfo.ServerInfoBuilder basicAuthEnabled(final boolean basicAuthEnabled) {
            this.basicAuthEnabled = basicAuthEnabled;
            return this;
        }

        /**
         * Indicates whether relay configuration is enabled for this server
         * @return {@code this}.
         */
        @JsonProperty("relayEnabled")
        public ServerInfo.ServerInfoBuilder relayEnabled(final boolean relayEnabled) {
            this.relayEnabled = relayEnabled;
            return this;
        }

        /**
         * Timestamp when the server was created
         * @return {@code this}.
         */
        @JsonProperty("createdAt")
        public ServerInfo.ServerInfoBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Current status of the server (e.g., "running", "stopped")
         * @return {@code this}.
         */
        @JsonProperty("status")
        public ServerInfo.ServerInfoBuilder status(final String status) {
            this.status = status;
            return this;
        }

        public ServerInfo build() {
            return new ServerInfo(this.serverId, this.port, this.description, this.protocol, this.baseUrl, this.tlsEnabled, this.mtlsEnabled, this.globalHeaders, this.basicAuthEnabled, this.relayEnabled, this.createdAt, this.status);
        }

        @Override
        public String toString() {
            return "ServerInfo.ServerInfoBuilder(serverId=" + this.serverId + ", port=" + this.port + ", description=" + this.description + ", protocol=" + this.protocol + ", baseUrl=" + this.baseUrl + ", tlsEnabled=" + this.tlsEnabled + ", mtlsEnabled=" + this.mtlsEnabled + ", globalHeaders=" + this.globalHeaders + ", basicAuthEnabled=" + this.basicAuthEnabled + ", relayEnabled=" + this.relayEnabled + ", createdAt=" + this.createdAt + ", status=" + this.status + ")";
        }
    }

    public static ServerInfo.ServerInfoBuilder builder() {
        return new ServerInfo.ServerInfoBuilder();
    }

    /**
     * Unique identifier of the server instance
     */
    public String getServerId() {
        return this.serverId;
    }

    /**
     * Port number the server is listening on
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Human-readable description of the server's purpose
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Protocol being used: "http" or "https"
     */
    public String getProtocol() {
        return this.protocol;
    }

    /**
     * Complete base URL for accessing the server (e.g., "https://localhost:1443")
     */
    public String getBaseUrl() {
        return this.baseUrl;
    }

    /**
     * Indicates whether TLS is enabled for this server
     */
    public boolean isTlsEnabled() {
        return this.tlsEnabled;
    }

    /**
     * Indicates whether mutual TLS (mTLS) is enabled for this server
     */
    public boolean isMtlsEnabled() {
        return this.mtlsEnabled;
    }

    /**
     * List of global headers configured for all responses from this server
     */
    public List<GlobalHeader> getGlobalHeaders() {
        return this.globalHeaders;
    }

    /**
     * Indicates whether basic authentication is enabled for this server
     */
    public boolean isBasicAuthEnabled() {
        return this.basicAuthEnabled;
    }

    /**
     * Indicates whether relay configuration is enabled for this server
     */
    public boolean isRelayEnabled() {
        return this.relayEnabled;
    }

    /**
     * Timestamp when the server was created
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Current status of the server (e.g., "running", "stopped")
     */
    public String getStatus() {
        return this.status;
    }

    public ServerInfo() {
    }

    /**
     * Creates a new {@code ServerInfo} instance.
     *
     * @param serverId Unique identifier of the server instance
     * @param port Port number the server is listening on
     * @param description Human-readable description of the server's purpose
     * @param protocol Protocol being used: "http" or "https"
     * @param baseUrl Complete base URL for accessing the server (e.g., "https://localhost:1443")
     * @param tlsEnabled Indicates whether TLS is enabled for this server
     * @param mtlsEnabled Indicates whether mutual TLS (mTLS) is enabled for this server
     * @param globalHeaders List of global headers configured for all responses from this server
     * @param basicAuthEnabled Indicates whether basic authentication is enabled for this server
     * @param relayEnabled Indicates whether relay configuration is enabled for this server
     * @param createdAt Timestamp when the server was created
     * @param status Current status of the server (e.g., "running", "stopped")
     */
    public ServerInfo(final String serverId, final int port, final String description, final String protocol, final String baseUrl, final boolean tlsEnabled, final boolean mtlsEnabled, final List<GlobalHeader> globalHeaders, final boolean basicAuthEnabled, final boolean relayEnabled, final LocalDateTime createdAt, final String status) {
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
}
