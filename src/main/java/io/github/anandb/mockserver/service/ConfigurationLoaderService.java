package io.github.anandb.mockserver.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.ServerConfiguration;
import io.github.anandb.mockserver.model.ServerCreationRequest;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import io.github.anandb.mockserver.util.JsonCommentParser;
import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.Validate;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Service responsible for loading server and expectation configurations from a JSON file.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationLoaderService {

    private final MockServerManager mockServerManager;
    private final ObjectMapper mapper;
    private final List<ResponseStrategy> strategies;

    private static final String CONFIG_FILE_PROPERTY = "mock.server.config.file";
    public static boolean SKIP_CONFIG_VALIDATIONS_FOR_TESTS = false;

    @PostConstruct
    public void loadConfigurationsOnStartup() {
        if (SKIP_CONFIG_VALIDATIONS_FOR_TESTS) {
            log.warn("Skipping Config Validations");
            return;
        }

        String configFilePath = System.getProperty(CONFIG_FILE_PROPERTY);
        Validate.isTrue(isNotBlank(configFilePath), "Server configuration required, set property " + CONFIG_FILE_PROPERTY);

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
            Validate.isTrue(configurations != null);

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
        MockServerOperations operations = new MockServerOperationsImpl(serverInstance.server());

        if (!isEmpty(serverInstance.relays())) {
            EnhancedExpectationDTO dto = EnhancedExpectationDTO.builder()
                    .httpRequest(mapper.createObjectNode())
                    .httpResponse(mapper.createObjectNode())
                    .build();
            configureExpectations(serverInstance, operations, List.of(dto));
        } else if (config.hasExpectations()) {
            configureExpectations(serverInstance, operations, config.getExpectations());
        }
    }

    private void configureExpectations(ServerInstance serverInstance,
                                        MockServerOperations operations,
                                        List<EnhancedExpectationDTO> expectations) {
        for (EnhancedExpectationDTO dto : expectations) {
            try {
                operations.configureEnhancedExpectation(
                        dto,
                        serverInstance.globalHeaders(),
                        strategies,
                        serverInstance.relays());
            } catch (Exception e) {
                log.error("Failed to configure expectation for server {}: {}", serverInstance.serverId(), e);
                throw e;
            }
        }
    }
}
