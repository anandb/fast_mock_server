package io.github.anandb.mockserver.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mockserver.integration.ClientAndServer;
import org.springframework.stereotype.Service;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.model.ServerCreationRequest;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing multiple MockServer instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockServerManager {

    private final TlsConfigurationService tlsConfigService;
    private final KubernetesTunnelService kubernetesTunnelService;
    private final List<ResponseStrategy> strategies;
    private Map<String, ServerInstance> servers = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown = false;

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!shuttingDown) {
                log.warn("JVM shutting down - forcing tunnel cleanup");
                forceKillTunnels();
            }
        }));
    }

    private void forceKillTunnels() {
        for (ServerInstance instance : servers.values()) {
            Map<String, Process> tunnels = instance.tunnels();
            if (tunnels != null) {
                for (Process tunnel : tunnels.values()) {
                    if (tunnel != null && tunnel.isAlive()) {
                        tunnel.destroyForcibly();
                    }
                }
            }
        }
    }

    public ServerInfo createServer(ServerCreationRequest request) {
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
                request.getRelays(),
                LocalDateTime.now(),
                request.getDescription()
            );

            if (request.isRelayEnabled()) {
                log.info("Configuring relay for server: {}", serverId);
                startTunnelsSequentially(instance, request.getRelays());
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
            stopTunnels(instance);
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

    private void startTunnelsSequentially(ServerInstance instance, List<RelayConfig> relays) {
        if (relays == null) {
            return;
        }

        for (RelayConfig relay : relays) {
            if (relay.isTunnelEnabled()) {
                try {
                    log.info("Starting tunnel for relay in namespace: {} with pod prefix: {}",
                            relay.getTunnelConfig().getNamespace(), relay.getTunnelConfig().getPodPrefix());

                    if (!kubernetesTunnelService.validateKubectl()) {
                        throw new ServerCreationException("kubectl is not installed or not accessible");
                    }

                    int hostPort = kubernetesTunnelService.findAvailablePort();
                    Process tunnelProcess = kubernetesTunnelService.startTunnel(relay.getTunnelConfig(), hostPort);

                    relay.setAssignedHostPort(hostPort);
                    instance.addTunnel(relay.getTunnelConfig().getNamespace() + ":" + relay.getTunnelConfig().getPodPrefix(), tunnelProcess);

                    log.info("Tunnel started on host port: {}", hostPort);
                } catch (Exception e) {
                    log.error("Failed to start tunnel for relay", e);
                    throw new ServerCreationException("Failed to start tunnel: " + e.getMessage(), e);
                }
            }
        }
    }

    private void stopTunnels(ServerInstance instance) {
        Map<String, Process> tunnels = instance.tunnels();
        if (tunnels != null && !tunnels.isEmpty()) {
            log.info("Stopping {} tunnels for server: {}", tunnels.size(), instance.serverId());
            for (Map.Entry<String, Process> entry : tunnels.entrySet()) {
                try {
                    kubernetesTunnelService.stopTunnel(entry.getValue());
                    log.debug("Stopped tunnel: {}", entry.getKey());
                } catch (Exception e) {
                    log.error("Failed to stop tunnel: {}", entry.getKey(), e);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
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
                .httpRequest(new com.fasterxml.jackson.databind.node.ObjectNode(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance)
                        .put("path", "/**"))
                .build();

        MockServerOperations operations = new MockServerOperationsImpl(instance.server());
        operations.configureEnhancedExpectation(relayDto, instance.globalHeaders(), strategies, instance.relays());
    }
}
