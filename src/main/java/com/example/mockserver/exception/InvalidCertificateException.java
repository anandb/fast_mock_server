package com.example.mockserver.exception;

/**
 * Exception thrown when certificate validation fails.
 * <p>
 * This exception indicates issues with TLS certificates, such as invalid format,
 * expired certificates, mismatched certificate-key pairs, or invalid CA certificates.
 * </p>
 */
public class InvalidCertificateException extends RuntimeException {

    /**
     * Constructs a new InvalidCertificateException with the specified detail message.
     *
     * @param message the detail message explaining why the certificate is invalid
     */
    public InvalidCertificateException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidCertificateException with the specified detail message and cause.
     *
     * @param message the detail message explaining why the certificate is invalid
     * @param cause the underlying cause of the validation failure
     */
    public InvalidCertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}
