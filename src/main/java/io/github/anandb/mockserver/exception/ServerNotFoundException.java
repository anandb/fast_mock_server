package io.github.anandb.mockserver.exception;

/**
 * Exception thrown when a requested mock server is not found in the registry.
 * <p>
 * This exception is typically thrown when attempting to access or manipulate a server
 * that doesn't exist or has been deleted.
 * </p>
 */
public class ServerNotFoundException extends RuntimeException {

    /**
     * Constructs a new ServerNotFoundException with a default message containing the server ID.
     *
     * @param serverId the unique identifier of the server that was not found
     */
    public ServerNotFoundException(String serverId) {
        super("Server not found with ID: " + serverId);
    }

    /**
     * Constructs a new ServerNotFoundException with a custom message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ServerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
