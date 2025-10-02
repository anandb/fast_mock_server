package com.example.mockserver.exception;

/**
 * Exception thrown when server creation fails
 */
public class ServerCreationException extends RuntimeException {

    public ServerCreationException(String message) {
        super(message);
    }

    public ServerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
