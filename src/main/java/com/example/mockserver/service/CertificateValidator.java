package com.example.mockserver.service;

import com.example.mockserver.exception.InvalidCertificateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Validates certificate content and format
 */
@Slf4j
@Component
public class CertificateValidator {

    private static final String PEM_CERT_BEGIN = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_CERT_END = "-----END CERTIFICATE-----";
    private static final String PEM_KEY_BEGIN = "-----BEGIN";
    private static final String PEM_KEY_END = "-----END";

    /**
     * Validate PEM format for certificate
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
     * Validate PEM format for private key
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
     * Validate CA certificate for mTLS
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
     * Validate certificate and private key pair
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
