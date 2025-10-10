package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response object containing information about a mock server instance.
 * <p>
 * Provides comprehensive details about a server's configuration, status, and runtime information.
 * Used in API responses to inform clients about server state and capabilities.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
