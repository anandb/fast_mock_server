package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.ServerCreationException;
import io.github.anandb.mockserver.model.ServerCreationRequest;
import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.ServerConfiguration;
import io.github.anandb.mockserver.model.ServerInstance;
import io.github.anandb.mockserver.util.JsonCommentParser;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

/**
 * Service responsible for loading server and expectation configurations from a JSON file or base64.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationLoaderService {

    private final MockServerManager mockServerManager;
    private final ObjectMapper objectMapper;
    private final List<ResponseStrategy> strategies;

    private static final String CONFIG_FILE_PROPERTY = "mock.server.config.file";
    private static final String CONFIG_FILE_B64_PROPERTY = "mock.server.config.fileb64";

    @PostConstruct
    public void loadConfigurationsOnStartup() {
        String configB64 = System.getProperty(CONFIG_FILE_B64_PROPERTY);
        if (configB64 != null && !configB64.trim().isEmpty()) {
            try {
                loadConfigurationsFromBase64(configB64);
                return;
            } catch (Exception e) {
                log.error("Failed to load configurations from base64", e);
                throw new ServerCreationException("Failed to load configurations from base64: " + e.getMessage(), e);
            }
        }

        String configFilePath = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configFilePath == null || configFilePath.trim().isEmpty()) {
            return;
        }

        File configFile = new File(configFilePath);
        if (!configFile.exists() || !configFile.isFile()) {
            log.warn("Configuration file not found or invalid: {}", configFilePath);
            return;
        }

        try {
            loadConfigurationsFromFile(configFile);
        } catch (Exception e) {
            log.error("Failed to load configurations from file: {}", configFilePath, e);
            throw new ServerCreationException("Failed to load configurations from file: " + configFilePath, e);
        }
    }

    private void loadConfigurationsFromBase64(String configB64) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(configB64.trim());
        String configContent = new String(decodedBytes, StandardCharsets.UTF_8);
        loadConfigurationsFromString(configContent, true);
    }

    private void loadConfigurationsFromFile(File configFile) throws IOException {
        boolean isJsonmc = configFile.getName().toLowerCase().endsWith(".jsonmc");
        String fileContent = Files.readString(configFile.toPath());
        loadConfigurationsFromString(fileContent, isJsonmc);
    }

    private void loadConfigurationsFromString(String configContent, boolean isJsonmc) throws IOException {
        if (!isJsonmc && (configContent.trim().startsWith("/*") || configContent.contains("//"))) {
            isJsonmc = true;
        }

        String jsonToParse = isJsonmc ? JsonCommentParser.clean(configContent) : configContent;
        ServerConfiguration[] configurations = objectMapper.readValue(jsonToParse, ServerConfiguration[].class);

        if (configurations == null) return;

        for (ServerConfiguration config : configurations) {
            try {
                processServerConfiguration(config);
            } catch (Exception e) {
                log.error("Failed to process server: {}", config.getServer().getServerId(), e);
            }
        }
    }

    private void processServerConfiguration(ServerConfiguration config) {
        ServerCreationRequest serverRequest = config.getServer();
        String serverId = serverRequest.getServerId();

        mockServerManager.createServer(serverRequest);
        ServerInstance serverInstance = mockServerManager.getServerInstance(serverId);
        MockServerOperations operations = new MockServerOperationsImpl(serverInstance.server());

        if (config.hasExpectations()) {
            for (EnhancedExpectationDTO dto : config.getExpectations()) {
                try {
                    operations.configureEnhancedExpectation(
                            dto,
                            serverInstance.globalHeaders(),
                            strategies
                    );
                } catch (Exception e) {
                    log.error("Failed to configure expectation for server {}: {}", serverId, e.getMessage());
                }
            }
        }
    }
}
