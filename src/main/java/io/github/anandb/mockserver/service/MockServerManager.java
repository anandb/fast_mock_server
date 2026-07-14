package io.github.anandb.mockserver.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mockserver.integration.ClientAndServer;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.EnhancedExpectation;
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
    private final MockServerOperationsFactory operationsFactory;
    private final List<ResponseStrategy> strategies;
    private Map<String, ServerInstance> servers = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown = false;

    private final Object serverCreationLock = new Object();

    public ServerInfo createServer(ServerCreationRequest request) {
        String serverId = request.getServerId();

        // Synchronized duplicate check — server creation is inherently slow
        // (network calls, file I/O), so the lock cost is negligible vs. the work.
        synchronized (serverCreationLock) {
            if (servers.containsKey(serverId)) {
                throw new ServerAlreadyExistsException(serverId);
            }
        }

        ClientAndServer server = null;
        try {
            log.info("Creating server: {} on port {}", serverId, request.getPort());

            // For TLS servers, we must hold the TLS lock from configuration through
            // server creation to prevent concurrent servers from overwriting the
            // global ConfigurationProperties.
            Object tlsLock = request.isTlsEnabled() ? tlsConfigService.getTlsConfigLock() : null;
            synchronized (tlsLock != null ? tlsLock : new Object()) {
                if (request.isTlsEnabled()) {
                    tlsConfigService.configureTls(serverId, request.getTlsConfig());
                }

                server = ClientAndServer.startClientAndServer(request.getPort());
            }

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

            // Register inside synchronized block to prevent duplicate creation
            synchronized (serverCreationLock) {
                if (servers.containsKey(serverId)) {
                    // Lost the race — another thread created this server
                    server.stop();
                    throw new ServerAlreadyExistsException(serverId);
                }
                servers.put(serverId, instance);
            }
            log.info("Successfully created server: {} at {}", serverId, instance.getBaseUrl());
            return toServerInfo(instance);

        } catch (Exception e) {
            log.error("Failed to create server: {}", serverId, e);
            if (server != null) {
                try {
                    server.stop();
                    log.info("Stopped partially created server on port {}", request.getPort());
                } catch (Exception stopEx) {
                    log.warn("Failed to stop partially created server on port {}", request.getPort(), stopEx);
                }
            }
            tlsConfigService.cleanupServerCertificates(serverId);
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

        cleanupResources(instance, serverId);
        log.info("Successfully deleted server: {}", serverId);
        return true;
    }

    private void cleanupResources(ServerInstance instance, String serverId) {
        // Best-effort cleanup: attempt all steps, log individual failures.
        // Never re-add the instance — partial cleanup leaves inconsistent state.
        boolean tunnelsStopped = stopTunnels(instance);
        boolean serverStopped = stopMockServer(instance);
        tlsConfigService.cleanupServerCertificates(serverId);

        if (!tunnelsStopped || !serverStopped) {
            log.warn("Partial cleanup for server: {} — tunnels={}, server={}",
                serverId, tunnelsStopped, serverStopped);
        }
    }

    private boolean stopMockServer(ServerInstance instance) {
        try {
            if (instance.server() != null && instance.server().isRunning()) {
                instance.server().stop();
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to stop mock server: {}", instance.serverId(), e);
            return false;
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

    private boolean stopTunnels(ServerInstance instance) {
        Map<String, Process> tunnels = instance.tunnels();
        if (tunnels == null || tunnels.isEmpty()) {
            return true;
        }

        log.info("Stopping {} tunnels for server: {}", tunnels.size(), instance.serverId());
        boolean allStopped = true;
        for (Map.Entry<String, Process> entry : tunnels.entrySet()) {
            try {
                kubernetesTunnelService.stopTunnel(entry.getValue());
                log.debug("Stopped tunnel: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Failed to stop tunnel: {}", entry.getKey(), e);
                allStopped = false;
            }
        }
        return allStopped;
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
        // Use a standard glob catch-all path
        ObjectNode requestNode = JsonNodeFactory.instance.objectNode();
        requestNode.put("path", "/**");

        EnhancedExpectation relayDto = EnhancedExpectation.builder()
                .httpRequest(requestNode)
                .build();

        MockServerOperations operations = operationsFactory.create(instance.server());
        // Use very low priority to ensure this acts as a fallback for specific expectations
        operations.configureEnhancedExpectation(relayDto, instance.globalHeaders(), strategies, instance.relays(), -1000);
    }
}
