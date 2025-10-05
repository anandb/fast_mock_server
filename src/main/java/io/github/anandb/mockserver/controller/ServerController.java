package io.github.anandb.mockserver.controller;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.service.MockServerManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing MockServer lifecycle.
 * <p>
 * Provides endpoints for creating, listing, retrieving, and deleting mock server instances.
 * Each server can be configured with custom ports, TLS/mTLS, and global headers.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@Validated
public class ServerController {

    private final MockServerManager mockServerManager;

    /**
     * Creates a new mock server instance with the specified configuration.
     *
     * @param request the server configuration including ID, port, TLS settings, and global headers
     * @return ResponseEntity containing the created server information
     * @throws ServerAlreadyExistsException if a server with the same ID already exists
     * @throws ServerCreationException if server creation fails
     */
    @PostMapping
    public ResponseEntity<ServerInfo> createServer(@Valid @RequestBody CreateServerRequest request) {
        log.info("Request to create server: {}", request.getServerId());
        ServerInfo serverInfo = mockServerManager.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(serverInfo);
    }

    /**
     * Lists all active mock server instances.
     *
     * @return ResponseEntity containing a list of all server information
     */
    @GetMapping
    public ResponseEntity<List<ServerInfo>> listServers() {
        log.debug("Request to list all servers");
        List<ServerInfo> servers = mockServerManager.listServers();
        return ResponseEntity.ok(servers);
    }

    /**
     * Retrieves detailed information about a specific mock server.
     *
     * @param serverId the unique identifier of the server
     * @return ResponseEntity containing the server information
     * @throws ServerNotFoundException if the server with the specified ID is not found
     */
    @GetMapping("/{serverId}")
    public ResponseEntity<ServerInfo> getServer(@PathVariable String serverId) {
        log.debug("Request to get server: {}", serverId);
        ServerInfo serverInfo = mockServerManager.getServerInfo(serverId);
        return ResponseEntity.ok(serverInfo);
    }

    /**
     * Deletes a mock server instance and cleans up associated resources.
     * <p>
     * This will stop the server, remove it from the registry, and clean up any
     * temporary certificate files.
     * </p>
     *
     * @param serverId the unique identifier of the server to delete
     * @return ResponseEntity with no content on successful deletion
     * @throws ServerNotFoundException if the server with the specified ID is not found
     */
    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServer(@PathVariable String serverId) {
        log.info("Request to delete server: {}", serverId);
        mockServerManager.deleteServer(serverId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Checks if a server with the specified ID exists.
     *
     * @param serverId the unique identifier of the server to check
     * @return ResponseEntity containing true if the server exists, false otherwise
     */
    @GetMapping("/{serverId}/exists")
    public ResponseEntity<Boolean> serverExists(@PathVariable String serverId) {
        boolean exists = mockServerManager.serverExists(serverId);
        return ResponseEntity.ok(exists);
    }
}
