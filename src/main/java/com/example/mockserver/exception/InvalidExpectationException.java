package com.example.mockserver.exception;

/**
 * Exception thrown when expectation JSON is invalid
 */
public class InvalidExpectationException extends RuntimeException {

    public InvalidExpectationException(String message) {
        super(message);
    }

    public InvalidExpectationException(String message, Throwable cause) {
        super(message, cause);
    }
}
