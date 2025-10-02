package com.example.mockserver.exception;

/**
 * Exception thrown when a requested mock server is not found
 */
public class ServerNotFoundException extends RuntimeException {

    public ServerNotFoundException(String serverId) {
        super("Server not found with ID: " + serverId);
    }

    public ServerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
