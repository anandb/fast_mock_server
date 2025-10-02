package com.example.mockserver.exception;

/**
 * Exception thrown when attempting to create a server with an ID that already exists
 */
public class ServerAlreadyExistsException extends RuntimeException {

    public ServerAlreadyExistsException(String serverId) {
        super("Server already exists with ID: " + serverId);
    }

    public ServerAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
