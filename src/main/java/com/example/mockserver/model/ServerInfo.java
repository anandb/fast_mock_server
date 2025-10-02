package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response object containing information about a mock server
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerInfo {

    @JsonProperty("serverId")
    private String serverId;

    @JsonProperty("port")
    private int port;

    @JsonProperty("description")
    private String description;

    @JsonProperty("protocol")
    private String protocol;  // "http" or "https"

    @JsonProperty("baseUrl")
    private String baseUrl;  // e.g., "https://localhost:1443"

    @JsonProperty("tlsEnabled")
    private boolean tlsEnabled;

    @JsonProperty("mtlsEnabled")
    private boolean mtlsEnabled;

    @JsonProperty("globalHeaders")
    private List<GlobalHeader> globalHeaders;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("status")
    private String status;  // "running", "stopped", etc.
}
