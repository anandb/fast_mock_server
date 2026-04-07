package io.github.anandb.mockserver;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.apache.commons.lang3.StringUtils.isBlank;

@SpringBootApplication
public class MockServerApplication {

    static {
        try {
            // Extract logging.properties to a temp file for JUL to use
            java.nio.file.Path tempLogConfig = java.nio.file.Files.createTempFile("logging", ".properties");
            try (var is = MockServerApplication.class.getResourceAsStream("/logging.properties");
                 var os = java.nio.file.Files.newOutputStream(tempLogConfig)) {
                if (is != null) {
                    is.transferTo(os);
                    System.setProperty("java.util.logging.config.file", tempLogConfig.toAbsolutePath().toString());
                    java.util.logging.LogManager.getLogManager().readConfiguration();
                }
            }
            // Bridge JUL to SLF4J
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            // Tell MockServer to stay quiet and avoid manual JUL config
            if (isBlank(System.getProperty("mockserver.logLevel", ""))) {
                System.setProperty("mockserver.logLevel", "WARN");
            }
        } catch (Exception ignored) {
            // Silently ignore if configuration fails
        }
    }

    public static void main(String[] args) {
        // If running as a container, see if a configuration has been copied/mounted
        File file = new File("/.dockerenv");
        if (file.exists() && isBlank(System.getProperty("mock.server.config.file", ""))) {
            System.setProperty("mock.server.config.file", "/server.jsonmc");
        }

        SpringApplication.run(MockServerApplication.class, args);
    }
}
