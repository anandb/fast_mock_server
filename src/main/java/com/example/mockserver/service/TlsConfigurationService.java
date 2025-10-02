package com.example.mockserver.service;

import com.example.mockserver.exception.InvalidCertificateException;
import com.example.mockserver.model.TlsConfig;
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
 * Service for managing TLS configuration and temporary certificate files
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TlsConfigurationService {

    private final CertificateValidator certificateValidator;

    @Value("${mockserver.cert.temp-dir:/tmp/mockserver-certs}")
    private String tempCertDir;

    @Value("${mockserver.cert.cleanup-on-shutdown:true}")
    private boolean cleanupOnShutdown;

    // Track certificate files for each server for cleanup
    private final Map<String, List<Path>> serverCertFiles = new ConcurrentHashMap<>();

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
     * Configure TLS for a MockServer instance
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
     * Configure mutual TLS (mTLS)
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
     * Write certificate content to a temporary file
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
     * Clean up certificate files for a specific server
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
     * Clean up all certificate files on shutdown
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
     * Get the number of certificate files for a server
     */
    public int getCertificateFileCount(String serverId) {
        List<Path> files = serverCertFiles.get(serverId);
        return files != null ? files.size() : 0;
    }
}
