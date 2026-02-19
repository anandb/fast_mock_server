package io.github.anandb.mockserver.controller;

import io.github.anandb.mockserver.exception.InvalidExpectationException;
import io.github.anandb.mockserver.exception.ServerNotFoundException;
import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.service.MockServerManager;
import io.github.anandb.mockserver.service.MockServerOperations;
import io.github.anandb.mockserver.service.MockServerOperationsImpl;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.mockserver.model.HttpRequest.request;

/**
 * REST controller for configuring expectations on mock servers.
 */
@Slf4j
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ExpectationController {

    private final MockServerManager mockServerManager;
    private final List<ResponseStrategy> strategies;
    private final ObjectMapper objectMapper;

    /**
     * Configures expectations for a specific mock server.
     *
     * @param serverId the unique identifier of the server
     * @param body JSON string containing one or more enhanced expectations
     * @return ResponseEntity with a success message
     */
    @PostMapping("/{serverId}/expectations")
    public ResponseEntity<String> configureExpectations(
            @PathVariable String serverId,
            @RequestBody String body
    ) {
        log.info("Configuring expectations for server: {}", serverId);

        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        MockServerOperations operations = new MockServerOperationsImpl(serverInstance.server());

        try {
            List<EnhancedExpectationDTO> dtos = parseExpectations(body);
            log.debug("Parsed {} expectations", dtos.size());

            for (EnhancedExpectationDTO dto : dtos) {
                // Clear existing expectations for same method/path if needed (optional, keeping current behavior)
                clearMatchingExpectation(serverInstance, dto.getHttpRequest());
                
                // Configure enhanced expectation
                operations.configureEnhancedExpectation(
                        dto,
                        serverInstance.globalHeaders(),
                        strategies
                );
            }

            String message = String.format("Successfully configured %d expectation(s) for server: %s", dtos.size(), serverId);
            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("Failed to configure expectations", e);
            throw new InvalidExpectationException("Failed to configure expectations: " + e.getMessage(), e);
        }
    }

    private List<EnhancedExpectationDTO> parseExpectations(String json) throws Exception {
        json = json.trim();
        if (json.startsWith("[")) {
            return objectMapper.readValue(json, new TypeReference<List<EnhancedExpectationDTO>>() {});
        } else {
            EnhancedExpectationDTO single = objectMapper.readValue(json, EnhancedExpectationDTO.class);
            List<EnhancedExpectationDTO> list = new ArrayList<>();
            list.add(single);
            return list;
        }
    }

    private void clearMatchingExpectation(ServerInstance instance, HttpRequest request) {
        if (request != null && request.getPath() != null) {
            HttpRequest matcher = request()
                    .withMethod(request.getMethod())
                    .withPath(request.getPath());
            instance.server().clear(matcher);
        }
    }

    @GetMapping(value = "/{serverId}/expectations", produces = "application/json")
    public ResponseEntity<Expectation[]> getExpectations(@PathVariable String serverId) {
        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        MockServerOperations operations = new MockServerOperationsImpl(serverInstance.server());
        return ResponseEntity.ok(operations.retrieveActiveExpectations(request()));
    }

    @DeleteMapping("/{serverId}/expectations")
    public ResponseEntity<String> clearExpectations(@PathVariable String serverId) {
        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        MockServerOperations operations = new MockServerOperationsImpl(serverInstance.server());
        operations.reset();
        return ResponseEntity.ok(String.format("Cleared all expectations for server: %s", serverId));
    }
}
