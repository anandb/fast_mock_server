package com.example.mockserver.service;

import com.example.mockserver.exception.ServerAlreadyExistsException;
import com.example.mockserver.exception.ServerCreationException;
import com.example.mockserver.exception.ServerNotFoundException;
import com.example.mockserver.model.CreateServerRequest;
import com.example.mockserver.model.GlobalHeader;
import com.example.mockserver.model.ServerInfo;
import com.example.mockserver.model.TlsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.integration.ClientAndServer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing multiple MockServer instances
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockServerManager {

    private final TlsConfigurationService tlsConfigService;

    // Registry of all active servers
    private final Map<String, ServerInstance> servers = new ConcurrentHashMap<>();

    /**
     * Create a new mock server
     */
    public ServerInfo createServer(CreateServerRequest request) {
        String serverId = request.getServerId();

        // Check if server already exists
        if (servers.containsKey(serverId)) {
            throw new ServerAlreadyExistsException(serverId);
        }

        try {
            log.info("Creating server: {} on port {}", serverId, request.getPort());

            // Configure TLS if provided
            if (request.isTlsEnabled()) {
                tlsConfigService.configureTls(serverId, request.getTlsConfig());
            }

            // Start MockServer
            ClientAndServer server = ClientAndServer.startClientAndServer(request.getPort());

            // Create server instance
            ServerInstance instance = new ServerInstance(
                serverId,
                request.getPort(),
                server,
                request.getTlsConfig(),
                request.getGlobalHeaders(),
                LocalDateTime.now(),
                request.getDescription()
            );

            // Store in registry
            servers.put(serverId, instance);

            log.info("Successfully created server: {} at {}", serverId, instance.getBaseUrl());

            return toServerInfo(instance);

        } catch (Exception e) {
            log.error("Failed to create server: {}", serverId, e);
            throw new ServerCreationException("Failed to create server: " + e.getMessage(), e);
        }
    }

    /**
     * Get a server instance by ID
     */
    public ServerInstance getServerInstance(String serverId) {
        ServerInstance instance = servers.get(serverId);
        if (instance == null) {
            throw new ServerNotFoundException(serverId);
        }
        return instance;
    }

    /**
     * Get server info by ID
     */
    public ServerInfo getServerInfo(String serverId) {
        return toServerInfo(getServerInstance(serverId));
    }

    /**
     * List all servers
     */
    public List<ServerInfo> listServers() {
        return servers.values().stream()
            .map(this::toServerInfo)
            .toList();
    }

    /**
     * Delete a server
     */
    public boolean deleteServer(String serverId) {
        ServerInstance instance = servers.remove(serverId);

        if (instance == null) {
            throw new ServerNotFoundException(serverId);
        }

        try {
            // Stop the MockServer
            instance.getServer().stop();
            log.info("Stopped MockServer for: {}", serverId);

            // Clean up certificate files
            tlsConfigService.cleanupServerCertificates(serverId);

            log.info("Successfully deleted server: {}", serverId);
            return true;

        } catch (Exception e) {
            log.error("Error while deleting server: {}", serverId, e);
            // Re-add to registry if stop failed
            servers.put(serverId, instance);
            throw new ServerCreationException("Failed to delete server: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a server exists
     */
    public boolean serverExists(String serverId) {
        return servers.containsKey(serverId);
    }

    /**
     * Get the count of active servers
     */
    public int getServerCount() {
        return servers.size();
    }

    /**
     * Convert ServerInstance to ServerInfo
     */
    private ServerInfo toServerInfo(ServerInstance instance) {
        return ServerInfo.builder()
            .serverId(instance.getServerId())
            .port(instance.getPort())
            .description(instance.getDescription())
            .protocol(instance.getProtocol())
            .baseUrl(instance.getBaseUrl())
            .tlsEnabled(instance.isTlsEnabled())
            .mtlsEnabled(instance.isMtlsEnabled())
            .globalHeaders(instance.getGlobalHeaders())
            .createdAt(instance.getCreatedAt())
            .status(instance.getServer().isRunning() ? "running" : "stopped")
            .build();
    }

    /**
     * Stop all servers on shutdown
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all MockServers...");
        List<String> serverIds = new ArrayList<>(servers.keySet());

        for (String serverId : serverIds) {
            try {
                deleteServer(serverId);
            } catch (Exception e) {
                log.error("Error stopping server during shutdown: {}", serverId, e);
            }
        }

        log.info("All MockServers stopped");
    }

    /**
     * Internal class representing a server instance
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ServerInstance {
        private String serverId;
        private int port;
        private ClientAndServer server;
        private TlsConfig tlsConfig;
        private List<GlobalHeader> globalHeaders;
        private LocalDateTime createdAt;
        private String description;

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

        public boolean hasGlobalHeaders() {
            return globalHeaders != null && !globalHeaders.isEmpty();
        }
    }
}
