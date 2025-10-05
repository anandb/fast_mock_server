package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.InvalidCertificateException;
import io.github.anandb.mockserver.model.TlsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.configuration.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing TLS configuration and temporary certificate files.
 * <p>
 * This service handles the creation and management of temporary certificate files
 * required for MockServer TLS/mTLS operation. It validates certificates, writes them
 * to temporary files with appropriate permissions, and ensures cleanup on shutdown.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TlsConfigurationService {

    private final CertificateValidator certificateValidator;

    /** Directory path for storing temporary certificate files */
    @Value("${mockserver.cert.temp-dir:/tmp/mockserver-certs}")
    private String tempCertDir;

    /** Whether to clean up certificate files on application shutdown */
    @Value("${mockserver.cert.cleanup-on-shutdown:true}")
    private boolean cleanupOnShutdown;

    /** Registry tracking certificate files for each server to enable cleanup */
    private final Map<String, List<Path>> serverCertFiles = new ConcurrentHashMap<>();

    /**
     * Initializes the TLS configuration service by creating the temporary certificate directory.
     * <p>
     * This method is automatically called by Spring after the bean is constructed.
     * </p>
     *
     * @throws RuntimeException if the temporary directory cannot be created
     */
    @PostConstruct
    public void init() {
        try {
            Path tempDir = Paths.get(tempCertDir);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                log.info("Created temp certificate directory: {}", tempCertDir);
            }
        } catch (IOException e) {
            log.error("Failed to create temp certificate directory", e);
            throw new RuntimeException("Failed to initialize TLS configuration service", e);
        }
    }

    /**
     * Configures TLS for a MockServer instance.
     * <p>
     * Validates the provided certificates, writes them to temporary files, and configures
     * the MockServer static configuration properties. If mTLS is included in the configuration,
     * it will also be configured.
     * </p>
     *
     * @param serverId the unique identifier of the server
     * @param tlsConfig the TLS configuration containing certificates and keys
     * @throws IOException if certificate files cannot be written
     * @throws InvalidCertificateException if certificate validation fails
     */
    public void configureTls(String serverId, TlsConfig tlsConfig) throws IOException {
        log.info("Configuring TLS for server: {}", serverId);

        // Validate certificates
        certificateValidator.validateCertificateKeyPair(
            tlsConfig.getCertificate(),
            tlsConfig.getPrivateKey()
        );

        // Write certificates to temp files
        String certPath = writeCertificateToTemp(serverId, tlsConfig.getCertificate(), "cert");
        String keyPath = writeCertificateToTemp(serverId, tlsConfig.getPrivateKey(), "key");

        // Configure MockServer with certificate paths
        ConfigurationProperties.certificateAuthorityCertificate(certPath);
        ConfigurationProperties.privateKeyPath(keyPath);

        log.info("TLS configured for server {} with cert: {} and key: {}", serverId, certPath, keyPath);

        // Configure mTLS if provided
        if (tlsConfig.hasMtls()) {
            configureMtls(serverId, tlsConfig);
        }
    }

    /**
     * Configures mutual TLS (mTLS) for client certificate authentication.
     * <p>
     * Validates the CA certificate and configures MockServer to require or accept
     * client certificates based on the mTLS configuration.
     * </p>
     *
     * @param serverId the unique identifier of the server
     * @param tlsConfig the TLS configuration containing mTLS settings
     * @throws IOException if the CA certificate file cannot be written
     * @throws InvalidCertificateException if CA certificate validation fails
     */
    private void configureMtls(String serverId, TlsConfig tlsConfig) throws IOException {
        log.info("Configuring mTLS for server: {}", serverId);

        // Validate CA certificate
        certificateValidator.validateCaCertificate(
            tlsConfig.getMtlsConfig().getCaCertificate()
        );

        // Write CA certificate to temp file
        String caCertPath = writeCertificateToTemp(
            serverId,
            tlsConfig.getMtlsConfig().getCaCertificate(),
            "ca"
        );

        // Configure MockServer for mTLS
        ConfigurationProperties.tlsMutualAuthenticationRequired(
            tlsConfig.getMtlsConfig().isRequireClientAuth()
        );
        ConfigurationProperties.tlsMutualAuthenticationCertificateChain(caCertPath);

        log.info("mTLS configured for server {} with CA cert: {}, requireClientAuth: {}",
            serverId, caCertPath, tlsConfig.getMtlsConfig().isRequireClientAuth());
    }

    /**
     * Writes certificate content to a temporary file with appropriate permissions.
     * <p>
     * Creates a temporary PEM file in the configured directory with restrictive permissions
     * (owner read/write only on Unix systems). The file path is tracked for later cleanup.
     * </p>
     *
     * @param serverId the unique identifier of the server owning this certificate
     * @param content the PEM-encoded certificate content
     * @param prefix a prefix for the file name (e.g., "cert", "key", "ca")
     * @return the absolute path to the created temporary file
     * @throws IOException if the file cannot be written
     * @throws InvalidCertificateException if the content is empty
     */
    public String writeCertificateToTemp(String serverId, String content, String prefix)
            throws IOException {

        if (content == null || content.trim().isEmpty()) {
            throw new InvalidCertificateException("Certificate content is empty");
        }

        // Create temp file
        Path tempFile = Files.createTempFile(
            Paths.get(tempCertDir),
            serverId + "-" + prefix + "-",
            ".pem"
        );

        // Write content
        Files.writeString(tempFile, content, StandardCharsets.UTF_8);

        // Set restrictive permissions (owner read/write only) on Unix systems
        try {
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                Files.setPosixFilePermissions(tempFile,
                    PosixFilePermissions.fromString("rw-------"));
                log.debug("Set restrictive permissions on: {}", tempFile);
            }
        } catch (UnsupportedOperationException e) {
            log.warn("Cannot set POSIX permissions on this file system");
        }

        // Track for cleanup
        serverCertFiles.computeIfAbsent(serverId, k -> new ArrayList<>()).add(tempFile);

        log.debug("Wrote {} certificate to temp file: {}", prefix, tempFile);
        return tempFile.toString();
    }

    /**
     * Cleans up all certificate files associated with a specific server.
     * <p>
     * Removes all temporary certificate files created for the server and deletes
     * them from the tracking registry.
     * </p>
     *
     * @param serverId the unique identifier of the server whose certificates should be cleaned up
     */
    public void cleanupServerCertificates(String serverId) {
        List<Path> files = serverCertFiles.remove(serverId);
        if (files != null) {
            for (Path path : files) {
                try {
                    if (Files.deleteIfExists(path)) {
                        log.debug("Deleted certificate file: {}", path);
                    }
                } catch (IOException e) {
                    log.warn("Failed to delete certificate file: {}", path, e);
                }
            }
            log.info("Cleaned up {} certificate files for server: {}", files.size(), serverId);
        }
    }

    /**
     * Cleans up all certificate files on application shutdown.
     * <p>
     * This method is automatically called by Spring when the application context is closing.
     * It removes all temporary certificate files if cleanup on shutdown is enabled.
     * </p>
     */
    @PreDestroy
    public void cleanup() {
        if (cleanupOnShutdown) {
            log.info("Cleaning up all certificate files...");
            List<String> serverIds = new ArrayList<>(serverCertFiles.keySet());
            for (String serverId : serverIds) {
                cleanupServerCertificates(serverId);
            }
            log.info("Certificate cleanup complete");
        }
    }

    /**
     * Gets the number of certificate files tracked for a specific server.
     * <p>
     * This is primarily useful for testing and monitoring purposes.
     * </p>
     *
     * @param serverId the unique identifier of the server
     * @return the number of certificate files tracked for this server, or 0 if none
     */
    public int getCertificateFileCount(String serverId) {
        List<Path> files = serverCertFiles.get(serverId);
        return files != null ? files.size() : 0;
    }
}
