package io.github.anandb.mockserver.exception;

/**
 * Exception thrown when OAuth2 token acquisition fails.
 * <p>
 * Indicates an error during communication with the OAuth2 token endpoint,
 * such as invalid credentials, non-200 responses, or malformed token responses.
 * </p>
 */
public class OAuth2Exception extends RuntimeException {

    public OAuth2Exception(String message) {
        super(message);
    }

    public OAuth2Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
