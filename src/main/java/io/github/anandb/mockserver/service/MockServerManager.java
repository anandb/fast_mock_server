package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.model.TlsConfig;
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
 * Service for managing multiple MockServer instances.
 * <p>
 * Provides lifecycle management for mock servers including creation, retrieval, deletion,
 * and status tracking. Maintains a registry of all active server instances and handles
 * TLS configuration through delegation to {@link TlsConfigurationService}.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockServerManager {

    private final TlsConfigurationService tlsConfigService;
    private final RelayService relayService;

    /** Registry of all active server instances, keyed by server ID */
    private final Map<String, ServerInstance> servers = new ConcurrentHashMap<>();

    /**
     * Creates a new mock server instance with the specified configuration.
     * <p>
     * This method configures TLS if requested, starts the MockServer instance,
     * and registers it in the active servers map.
     * </p>
     *
     * @param request the server creation request containing configuration details
     * @return ServerInfo object containing details about the created server
     * @throws ServerAlreadyExistsException if a server with the same ID already exists
     * @throws ServerCreationException if server creation fails due to configuration or runtime errors
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

            // Configure relay if enabled
            if (request.isRelayEnabled()) {
                log.info("Configuring relay for server: {}", serverId);
                configureRelay(server, request.getRelayConfig());
            }

            // Create server instance
            ServerInstance instance = new ServerInstance(
                serverId,
                request.getPort(),
                server,
                request.getTlsConfig(),
                request.getGlobalHeaders(),
                request.getBasicAuthConfig(),
                request.getRelayConfig(),
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
     * Retrieves a server instance by its unique identifier.
     *
     * @param serverId the unique identifier of the server
     * @return the ServerInstance object
     * @throws ServerNotFoundException if no server with the specified ID exists
     */
    public ServerInstance getServerInstance(String serverId) {
        ServerInstance instance = servers.get(serverId);
        if (instance == null) {
            throw new ServerNotFoundException(serverId);
        }
        return instance;
    }

    /**
     * Retrieves server information by its unique identifier.
     *
     * @param serverId the unique identifier of the server
     * @return ServerInfo object containing details about the server
     * @throws ServerNotFoundException if no server with the specified ID exists
     */
    public ServerInfo getServerInfo(String serverId) {
        return toServerInfo(getServerInstance(serverId));
    }

    /**
     * Lists all active mock server instances.
     *
     * @return list of ServerInfo objects for all active servers
     */
    public List<ServerInfo> listServers() {
        return servers.values().stream()
            .map(this::toServerInfo)
            .toList();
    }

    /**
     * Deletes a mock server instance and cleans up associated resources.
     * <p>
     * This method stops the server, removes it from the registry, and cleans up
     * any temporary certificate files created for the server.
     * </p>
     *
     * @param serverId the unique identifier of the server to delete
     * @return true if the server was successfully deleted
     * @throws ServerNotFoundException if no server with the specified ID exists
     * @throws ServerCreationException if the server cannot be stopped or cleaned up
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
     * Checks if a server with the specified ID exists in the registry.
     *
     * @param serverId the unique identifier of the server to check
     * @return true if the server exists, false otherwise
     */
    public boolean serverExists(String serverId) {
        return servers.containsKey(serverId);
    }

    /**
     * Gets the count of active mock server instances.
     *
     * @return the number of servers currently in the registry
     */
    public int getServerCount() {
        return servers.size();
    }

    /**
     * Converts a ServerInstance to a ServerInfo DTO.
     * <p>
     * This method transforms the internal server representation into the public API response format.
     * </p>
     *
     * @param instance the ServerInstance to convert
     * @return ServerInfo DTO containing the server's public information
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
            .basicAuthEnabled(instance.isBasicAuthEnabled())
            .relayEnabled(instance.isRelayEnabled())
            .createdAt(instance.getCreatedAt())
            .status(instance.getServer().isRunning() ? "running" : "stopped")
            .build();
    }

    /**
     * Stops all active mock servers during application shutdown.
     * <p>
     * This method is automatically called by Spring when the application context is closing.
     * It ensures all servers are properly stopped and resources are cleaned up.
     * </p>
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
     * Internal class representing a server instance with all its runtime information.
     * <p>
     * This class encapsulates the MockServer instance along with its configuration
     * and metadata. It provides convenience methods for determining protocol, URLs,
     * and feature availability.
     * </p>
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ServerInstance {
        /** Unique identifier of the server */
        private String serverId;
        /** Port number the server is listening on */
        private int port;
        /** The underlying MockServer instance */
        private ClientAndServer server;
        /** TLS configuration, if enabled */
        private TlsConfig tlsConfig;
        /** List of global headers to apply to all responses */
        private List<GlobalHeader> globalHeaders;
        /** Basic authentication configuration, if enabled */
        private io.github.anandb.mockserver.model.BasicAuthConfig basicAuthConfig;
        /** Relay configuration, if enabled */
        private RelayConfig relayConfig;
        /** Timestamp when the server was created */
        private LocalDateTime createdAt;
        /** Human-readable description of the server */
        private String description;

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
         * Checks if global headers are configured for this server.
         *
         * @return true if at least one global header is configured, false otherwise
         */
        public boolean hasGlobalHeaders() {
            return globalHeaders != null && !globalHeaders.isEmpty();
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
    }

    /**
     * Configures a catch-all relay expectation for the server.
     * <p>
     * This sets up a callback that intercepts all requests and forwards them to
     * the configured remote server with OAuth2 authentication.
     * </p>
     *
     * @param server the MockServer instance
     * @param relayConfig the relay configuration
     */
    private void configureRelay(ClientAndServer server, RelayConfig relayConfig) {
        io.github.anandb.mockserver.callback.RelayResponseCallback callback =
            new io.github.anandb.mockserver.callback.RelayResponseCallback(relayService, relayConfig);

        // Create a catch-all request matcher
        org.mockserver.model.HttpRequest catchAllRequest = org.mockserver.model.HttpRequest.request();

        // Configure the callback for all requests
        server.when(catchAllRequest).respond(callback);

        log.info("Relay configured for remote URL: {}", relayConfig.getRemoteUrl());
    }
}
