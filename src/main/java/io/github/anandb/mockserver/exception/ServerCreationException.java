package io.github.anandb.mockserver.exception;

/**
 * Exception thrown when server creation or deletion fails.
 * <p>
 * This exception indicates that an error occurred during the creation of a new mock server
 * instance or during the cleanup process when deleting a server. Common causes include
 * port conflicts, configuration issues, or system resource problems.
 * </p>
 */
public class ServerCreationException extends RuntimeException {

    /**
     * Constructs a new ServerCreationException with the specified detail message.
     *
     * @param message the detail message explaining why the server operation failed
     */
    public ServerCreationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ServerCreationException with the specified detail message and cause.
     *
     * @param message the detail message explaining why the server operation failed
     * @param cause the underlying cause of the failure
     */
    public ServerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
