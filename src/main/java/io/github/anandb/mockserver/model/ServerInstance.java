package io.github.anandb.mockserver.model;

import org.mockserver.integration.ClientAndServer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerInstance {
    private final String serverId;
    private final int port;
    private final ClientAndServer server;
    private final TlsConfig tlsConfig;
    private final List<GlobalHeader> globalHeaders;
    private final BasicAuthConfig basicAuthConfig;
    private final List<RelayConfig> relays;
    private final LocalDateTime createdAt;
    private final String description;
    private final Map<String, Process> tunnels;

    public ServerInstance(
            String serverId,
            int port,
            ClientAndServer server,
            TlsConfig tlsConfig,
            List<GlobalHeader> globalHeaders,
            BasicAuthConfig basicAuthConfig,
            List<RelayConfig> relays,
            LocalDateTime createdAt,
            String description) {
        this.serverId = serverId;
        this.port = port;
        this.server = server;
        this.tlsConfig = tlsConfig;
        this.globalHeaders = globalHeaders;
        this.basicAuthConfig = basicAuthConfig;
        this.relays = relays;
        this.createdAt = createdAt;
        this.description = description;
        this.tunnels = new HashMap<>();
    }

    public String serverId() {
        return serverId;
    }

    public int port() {
        return port;
    }

    public ClientAndServer server() {
        return server;
    }

    public TlsConfig tlsConfig() {
        return tlsConfig;
    }

    public List<GlobalHeader> globalHeaders() {
        return globalHeaders;
    }

    public BasicAuthConfig basicAuthConfig() {
        return basicAuthConfig;
    }

    public List<RelayConfig> relays() {
        return relays;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public String description() {
        return description;
    }

    public Map<String, Process> tunnels() {
        return tunnels;
    }

    public void addTunnel(String key, Process process) {
        tunnels.put(key, process);
    }

    public Process getTunnel(String key) {
        return tunnels.get(key);
    }

    public void removeTunnel(String key) {
        tunnels.remove(key);
    }

    public String getProtocol() {
        return (tlsConfig != null && tlsConfig.isValid()) ? "https" : "http";
    }

    public String getBaseUrl() {
        return String.format("%s://localhost:%d", getProtocol(), port);
    }

    public boolean isTlsEnabled() {
        return tlsConfig != null && tlsConfig.isValid();
    }

    public boolean isMtlsEnabled() {
        return tlsConfig != null && tlsConfig.hasMtls();
    }

    public boolean isBasicAuthEnabled() {
        return basicAuthConfig != null && basicAuthConfig.isValid();
    }

    public boolean isRelayEnabled() {
        return relays != null && !relays.isEmpty() && relays.stream().allMatch(RelayConfig::isValid);
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
