package io.github.anandb.mockserver;

import java.io.File;

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

        // If running as a container, see if a configuration has been copied/mounted
        if ("/home/cnb".equals(System.getProperty("user.home"))) {
            String filePath = "/home/cnb/server.jsonmc";
            File file = new File(filePath);
            if (file.exists()) {
                System.setProperty("mock.server.config.file", filePath);
            }
        }

        SpringApplication.run(MockServerApplication.class, args);
    }
}
