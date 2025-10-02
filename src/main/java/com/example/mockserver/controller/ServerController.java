package com.example.mockserver.controller;

import com.example.mockserver.model.CreateServerRequest;
import com.example.mockserver.model.ServerInfo;
import com.example.mockserver.service.MockServerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for managing MockServer lifecycle
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@Validated
public class ServerController {

    private final MockServerManager mockServerManager;

    /**
     * Create a new mock server
     */
    @PostMapping
    public ResponseEntity<ServerInfo> createServer(@Valid @RequestBody CreateServerRequest request) {
        log.info("Request to create server: {}", request.getServerId());
        ServerInfo serverInfo = mockServerManager.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(serverInfo);
    }

    /**
     * List all servers
     */
    @GetMapping
    public ResponseEntity<List<ServerInfo>> listServers() {
        log.debug("Request to list all servers");
        List<ServerInfo> servers = mockServerManager.listServers();
        return ResponseEntity.ok(servers);
    }

    /**
     * Get server details by ID
     */
    @GetMapping("/{serverId}")
    public ResponseEntity<ServerInfo> getServer(@PathVariable String serverId) {
        log.debug("Request to get server: {}", serverId);
        ServerInfo serverInfo = mockServerManager.getServerInfo(serverId);
        return ResponseEntity.ok(serverInfo);
    }

    /**
     * Delete a server
     */
    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServer(@PathVariable String serverId) {
        log.info("Request to delete server: {}", serverId);
        mockServerManager.deleteServer(serverId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if a server exists
     */
    @GetMapping("/{serverId}/exists")
    public ResponseEntity<Boolean> serverExists(@PathVariable String serverId) {
        boolean exists = mockServerManager.serverExists(serverId);
        return ResponseEntity.ok(exists);
    }
}
