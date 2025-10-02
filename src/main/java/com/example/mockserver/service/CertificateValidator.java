package com.example.mockserver.service;

import com.example.mockserver.exception.InvalidCertificateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Validates certificate content and format for TLS/mTLS configuration.
 * <p>
 * Performs validation of PEM-encoded certificates and private keys to ensure they are
 * properly formatted and valid before being used in MockServer configuration. Validates
 * certificate expiration dates and basic constraints for CA certificates.
 * </p>
 */
@Slf4j
@Component
public class CertificateValidator {

    private static final String PEM_CERT_BEGIN = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_CERT_END = "-----END CERTIFICATE-----";
    private static final String PEM_KEY_BEGIN = "-----BEGIN";
    private static final String PEM_KEY_END = "-----END";

    /**
     * Validates the PEM format and validity of a certificate.
     * <p>
     * Checks for proper PEM markers, parses the certificate to verify it's valid X.509,
     * and validates that the certificate is within its validity period.
     * </p>
     *
     * @param certificate the PEM-encoded certificate to validate
     * @throws InvalidCertificateException if the certificate is empty, improperly formatted,
     *         invalid, or expired
     */
    public void validateCertificateFormat(String certificate) {
        if (certificate == null || certificate.trim().isEmpty()) {
            throw new InvalidCertificateException("Certificate content is empty");
        }

        if (!certificate.contains(PEM_CERT_BEGIN) || !certificate.contains(PEM_CERT_END)) {
            throw new InvalidCertificateException(
                "Certificate must be in PEM format with BEGIN/END CERTIFICATE markers"
            );
        }

        // Try to parse the certificate
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(certificate.getBytes())
            );

            // Check validity dates
            cert.checkValidity();

            log.debug("Certificate validated successfully. Subject: {}", cert.getSubjectX500Principal());

        } catch (CertificateException e) {
            throw new InvalidCertificateException(
                "Invalid certificate: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Validates the PEM format of a private key.
     * <p>
     * Checks for proper PEM markers and validates that the key is one of the supported
     * types (PRIVATE KEY, RSA PRIVATE KEY, or EC PRIVATE KEY).
     * </p>
     *
     * @param privateKey the PEM-encoded private key to validate
     * @throws InvalidCertificateException if the private key is empty, improperly formatted,
     *         or not a recognized key type
     */
    public void validatePrivateKeyFormat(String privateKey) {
        if (privateKey == null || privateKey.trim().isEmpty()) {
            throw new InvalidCertificateException("Private key content is empty");
        }

        if (!privateKey.contains(PEM_KEY_BEGIN) || !privateKey.contains(PEM_KEY_END)) {
            throw new InvalidCertificateException(
                "Private key must be in PEM format with BEGIN/END markers"
            );
        }

        // Basic validation - checking for common key types
        boolean isValidKeyType = privateKey.contains("BEGIN PRIVATE KEY") ||
                                 privateKey.contains("BEGIN RSA PRIVATE KEY") ||
                                 privateKey.contains("BEGIN EC PRIVATE KEY");

        if (!isValidKeyType) {
            throw new InvalidCertificateException(
                "Unrecognized private key format. Expected PRIVATE KEY, RSA PRIVATE KEY, or EC PRIVATE KEY"
            );
        }

        log.debug("Private key format validated successfully");
    }

    /**
     * Validates a CA certificate for use in mTLS configuration.
     * <p>
     * Verifies the certificate is properly formatted, valid X.509, and checks if it has
     * the basic constraints indicating it's a CA certificate (though this is logged as
     * a warning rather than failing validation).
     * </p>
     *
     * @param caCertificate the PEM-encoded CA certificate to validate
     * @throws InvalidCertificateException if the certificate is empty, improperly formatted,
     *         or invalid
     */
    public void validateCaCertificate(String caCertificate) {
        if (caCertificate == null || caCertificate.trim().isEmpty()) {
            throw new InvalidCertificateException("CA certificate content is empty");
        }

        if (!caCertificate.contains(PEM_CERT_BEGIN) || !caCertificate.contains(PEM_CERT_END)) {
            throw new InvalidCertificateException(
                "CA certificate must be in PEM format with BEGIN/END CERTIFICATE markers"
            );
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(caCertificate.getBytes())
            );

            // Verify it's a CA certificate
            int pathLen = cert.getBasicConstraints();
            if (pathLen < 0) {
                log.warn("Certificate may not be a CA certificate (basicConstraints < 0)");
            }

            log.debug("CA certificate validated successfully. Subject: {}", cert.getSubjectX500Principal());

        } catch (CertificateException e) {
            throw new InvalidCertificateException(
                "Invalid CA certificate: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Validates a certificate and private key pair.
     * <p>
     * Performs individual validation on both the certificate and private key to ensure
     * they are properly formatted and valid. Additional cryptographic verification of
     * the key pair match could be added in the future.
     * </p>
     *
     * @param certificate the PEM-encoded certificate to validate
     * @param privateKey the PEM-encoded private key to validate
     * @throws InvalidCertificateException if either the certificate or private key is invalid
     */
    public void validateCertificateKeyPair(String certificate, String privateKey) {
        // First validate formats individually
        validateCertificateFormat(certificate);
        validatePrivateKeyFormat(privateKey);

        // Additional validation could be added here to verify the key pair matches
        // This would require more complex cryptographic operations
        log.debug("Certificate and private key pair validated");
    }
}
