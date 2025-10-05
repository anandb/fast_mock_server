package io.github.anandb.mockserver.exception;

/**
 * Exception thrown when expectation JSON is invalid or cannot be parsed.
 * <p>
 * This exception indicates issues with the expectation configuration, such as
 * malformed JSON, invalid expectation format, or missing required fields.
 * </p>
 */
public class InvalidExpectationException extends RuntimeException {

    /**
     * Constructs a new InvalidExpectationException with the specified detail message.
     *
     * @param message the detail message explaining why the expectation is invalid
     */
    public InvalidExpectationException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidExpectationException with the specified detail message and cause.
     *
     * @param message the detail message explaining why the expectation is invalid
     * @param cause the underlying cause of the parsing or validation failure
     */
    public InvalidExpectationException(String message, Throwable cause) {
        super(message, cause);
    }
}
