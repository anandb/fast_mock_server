package io.github.anandb.mockserver.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.model.EnhancedExpectation;
import io.github.anandb.mockserver.model.ServerConfiguration;
import io.github.anandb.mockserver.model.ServerCreationRequest;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import io.github.anandb.mockserver.util.JsonCommentParser;
import jakarta.annotation.PostConstruct;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * Service responsible for loading server and expectation configurations from a JSON file.
 */
@Service
public class ConfigurationLoaderService {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationLoaderService.class);
    private final MockServerManager mockServerManager;
    private final MockServerOperationsFactory operationsFactory;
    private final ObjectMapper mapper;
    private final List<ResponseStrategy> strategies;
    private static final String CONFIG_FILE_PROPERTY = "mock.server.config.file";

    @PostConstruct
    public void loadConfigurationsOnStartup() {
        String configFilePath = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configFilePath == null || configFilePath.isBlank()) {
            log.info("No configuration file specified via {}, skipping config loading", CONFIG_FILE_PROPERTY);
            return;
        }
        File configFile = new File(configFilePath);
        if (!configFile.exists() || !configFile.isFile()) {
            log.warn("Configuration file not found or invalid: {}", configFilePath);
            throw new ServerCreationException("Failed to locate configuration file: " + configFilePath);
        }
        try {
            loadConfigurationsFromFile(configFile);
        } catch (Exception e) {
            log.error("Failed to load configurations from file: {}", configFilePath, e);
            throw new ServerCreationException("Failed to load configurations from file: " + configFilePath, e);
        }
    }

    private void loadConfigurationsFromFile(File configFile) throws IOException {
        try {
            String fileName = configFile.getName().toLowerCase();
            String fileContent = Files.readString(configFile.toPath());
            boolean isJsonmc = fileName.endsWith(".jsonmc") || fileName.endsWith(".jsonc");
            String jsonToParse = isJsonmc ? JsonCommentParser.clean(fileContent) : fileContent;
            ServerConfiguration[] configurations = mapper.readValue(jsonToParse, ServerConfiguration[].class);
            if (configurations == null || configurations.length == 0) {
                log.warn("No server configurations found in file: {}", configFile.getName());
                return;
            }
            for (ServerConfiguration config : configurations) {
                processServerConfiguration(config);
            }
        } catch (Exception e) {
            log.error("Failed to process server configuration", e);
            throw e;
        }
    }

    private void processServerConfiguration(ServerConfiguration config) {
        ServerCreationRequest serverRequest = config.getServer();
        String serverId = serverRequest.getServerId();
        mockServerManager.createServer(serverRequest);
        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        MockServerOperations operations = operationsFactory.create(serverInstance.server());
        if (!isEmpty(serverInstance.relays())) {
            EnhancedExpectation dto = EnhancedExpectation.builder().httpRequest(mapper.createObjectNode()).httpResponse(mapper.createObjectNode()).build();
            configureExpectations(serverInstance, operations, List.of(dto));
        }
        if (config.hasExpectations()) {
            configureExpectations(serverInstance, operations, config.getExpectations());
        }
    }

    private void configureExpectations(ServerInstance serverInstance, MockServerOperations operations, List<EnhancedExpectation> expectations) {
        for (EnhancedExpectation dto : expectations) {
            try {
                operations.configureEnhancedExpectation(dto, serverInstance.globalHeaders(), strategies, serverInstance.relays());
            } catch (Exception e) {
                log.error("Failed to configure expectation for server {}: {}", serverInstance.serverId(), e);
                throw e;
            }
        }
    }

    public ConfigurationLoaderService(final MockServerManager mockServerManager, final MockServerOperationsFactory operationsFactory, final ObjectMapper mapper, final List<ResponseStrategy> strategies) {
        this.mockServerManager = mockServerManager;
        this.operationsFactory = operationsFactory;
        this.mapper = mapper;
        this.strategies = strategies;
    }
}
