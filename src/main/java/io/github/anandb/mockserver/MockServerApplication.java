package io.github.anandb.mockserver;

import java.util.logging.LogManager;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for managing multiple MockServer instances.
 * <p>
 * This application provides REST APIs to create, manage, and configure multiple
 * MockServer instances with support for TLS/mTLS, global headers, and custom expectations.
 * </p>
 */
@SpringBootApplication
public class MockServerApplication {

    /**
     * Main entry point for the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        // Optionally remove existing JUL Handlers to prevent duplicate logging
        LogManager.getLogManager().reset();

        // Install the SLF4JBridgeHandler
        SLF4JBridgeHandler.install();

        SpringApplication.run(MockServerApplication.class, args);
    }
}
