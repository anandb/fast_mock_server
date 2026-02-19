package io.github.anandb.mockserver.controller;

import io.github.anandb.mockserver.exception.ServerAlreadyExistsException;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.CreateServerRequest;
import io.github.anandb.mockserver.model.ServerInfo;
import io.github.anandb.mockserver.model.ServerInstance;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
@Validated
public class ServerController {

    private final MockServerManager mockServerManager;

    @PostMapping
    public ResponseEntity<ServerInfo> createServer(@Valid @RequestBody CreateServerRequest request) {
        log.info("Request to create server: {}", request.getServerId());
        ServerInfo serverInfo = mockServerManager.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(serverInfo);
    }

    @GetMapping
    public ResponseEntity<List<ServerInfo>> listServers() {
        log.debug("Request to list all servers");
        List<ServerInfo> servers = mockServerManager.listServers();
        return ResponseEntity.ok(servers);
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ServerInfo> getServer(@PathVariable String serverId) {
        log.debug("Request to get server: {}", serverId);
        ServerInfo serverInfo = mockServerManager.getServerInfo(serverId);
        return ResponseEntity.ok(serverInfo);
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<Void> deleteServer(@PathVariable String serverId) {
        log.info("Request to delete server: {}", serverId);
        mockServerManager.deleteServer(serverId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{serverId}/exists")
    public ResponseEntity<Boolean> serverExists(@PathVariable String serverId) {
        boolean exists = mockServerManager.serverExists(serverId);
        return ResponseEntity.ok(exists);
    }
}
