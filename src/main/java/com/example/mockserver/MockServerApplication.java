package com.example.mockserver;

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
        SpringApplication.run(MockServerApplication.class, args);
    }
}
