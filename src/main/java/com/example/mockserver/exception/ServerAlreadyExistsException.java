package com.example.mockserver.exception;

/**
 * Exception thrown when attempting to create a server with an ID that already exists in the registry.
 * <p>
 * This exception indicates that a server with the specified ID is already running and
 * prevents duplicate server instances with the same identifier.
 * </p>
 */
public class ServerAlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new ServerAlreadyExistsException with a default message containing the server ID.
     *
     * @param serverId the unique identifier of the server that already exists
     */
    public ServerAlreadyExistsException(String serverId) {
        super("Server already exists with ID: " + serverId);
    }

    /**
     * Constructs a new ServerAlreadyExistsException with a custom message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ServerAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
