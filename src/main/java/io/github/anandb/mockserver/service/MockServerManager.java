package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockServerManager {

    private final TlsConfigurationService tlsConfigService;
    private final RelayService relayService;
    private final List<ResponseStrategy> strategies;

    private final Map<String, ServerInstance> servers = new ConcurrentHashMap<>();

    public ServerInfo createServer(CreateServerRequest request) {
        String serverId = request.getServerId();

        if (servers.containsKey(serverId)) {
            throw new ServerAlreadyExistsException(serverId);
        }

        try {
            log.info("Creating server: {} on port {}", serverId, request.getPort());

            if (request.isTlsEnabled()) {
                tlsConfigService.configureTls(serverId, request.getTlsConfig());
            }

            ClientAndServer server = ClientAndServer.startClientAndServer(request.getPort());

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

            if (request.isRelayEnabled()) {
                log.info("Configuring relay for server: {}", serverId);
                configureRelay(instance);
            }

            servers.put(serverId, instance);
            log.info("Successfully created server: {} at {}", serverId, instance.getBaseUrl());
            return toServerInfo(instance);

        } catch (Exception e) {
            log.error("Failed to create server: {}", serverId, e);
            throw new ServerCreationException("Failed to create server: " + e.getMessage(), e);
        }
    }

    public ServerInstance getServerInstance(String serverId) {
        ServerInstance instance = servers.get(serverId);
        if (instance == null) {
            throw new ServerNotFoundException(serverId);
        }
        return instance;
    }

    public ServerInfo getServerInfo(String serverId) {
        return toServerInfo(getServerInstance(serverId));
    }

    public List<ServerInfo> listServers() {
        return servers.values().stream()
            .map(this::toServerInfo)
            .toList();
    }

    public boolean deleteServer(String serverId) {
        ServerInstance instance = servers.remove(serverId);
        if (instance == null) {
            throw new ServerNotFoundException(serverId);
        }

        try {
            instance.server().stop();
            tlsConfigService.cleanupServerCertificates(serverId);
            log.info("Successfully deleted server: {}", serverId);
            return true;
        } catch (Exception e) {
            log.error("Error while deleting server: {}", serverId, e);
            servers.put(serverId, instance);
            throw new ServerCreationException("Failed to delete server: " + e.getMessage(), e);
        }
    }

    public boolean serverExists(String serverId) {
        return servers.containsKey(serverId);
    }

    public int getServerCount() {
        return servers.size();
    }

    private ServerInfo toServerInfo(ServerInstance instance) {
        return ServerInfo.builder()
            .serverId(instance.serverId())
            .port(instance.port())
            .description(instance.description())
            .protocol(instance.getProtocol())
            .baseUrl(instance.getBaseUrl())
            .tlsEnabled(instance.isTlsEnabled())
            .mtlsEnabled(instance.isMtlsEnabled())
            .globalHeaders(instance.globalHeaders())
            .basicAuthEnabled(instance.isBasicAuthEnabled())
            .relayEnabled(instance.isRelayEnabled())
            .createdAt(instance.createdAt())
            .status(instance.isRunning() ? "running" : "stopped")
            .build();
    }

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
    }

    private void configureRelay(ServerInstance instance) {
        EnhancedExpectationDTO relayDto = EnhancedExpectationDTO.builder()
                .relay(instance.relayConfig())
                .build();

        MockServerOperations operations = new MockServerOperationsImpl(instance.server());
        operations.configureEnhancedExpectation(relayDto, instance.globalHeaders(), strategies);
    }
}
