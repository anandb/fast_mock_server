package com.example.mockserver.exception;

/**
 * Exception thrown when certificate validation fails
 */
public class InvalidCertificateException extends RuntimeException {

    public InvalidCertificateException(String message) {
        super(message);
    }

    public InvalidCertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}
