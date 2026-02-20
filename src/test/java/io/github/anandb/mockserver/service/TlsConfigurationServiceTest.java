package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.InvalidCertificateException;
import io.github.anandb.mockserver.model.MtlsConfig;
import io.github.anandb.mockserver.model.TlsConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for TlsConfigurationService.
 * <p>
 * Tests TLS configuration, certificate file management, and cleanup operations.
 * </p>
 */
@DisplayName("TlsConfigurationService Tests")
class TlsConfigurationServiceTest {

    @Mock
    private CertificateValidator certificateValidator;

    private TlsConfigurationService tlsConfigService;

    @TempDir
    private Path tempDir;

    private static final String VALID_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIDazCCAlOgAwIBAgIUXZ8VLqkqE9XqJZ3pN3VWJqYKhEQwDQYJKoZIhvcNAQEL
        BQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
        GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yNTAxMDEwMDAwMDBaFw0yNjAx
        MDEwMDAwMDBaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
        HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
        AQUAA4IBDwAwggEKAoIBAQC7VJTUt9Us8cKjMzEfYyjiWA4R4/M2bS1+fWIcPm7t
        KEBLkLj6Y9pHJlYhBLJ3JkPvXgKqEPgLGGMlSKOXJi7eFGdNJcYmK3dqXl3hcr3U
        6v0yxvMqxSQLXZXqdxGTxMRdNVPq
        -----END CERTIFICATE-----
        """;

    private static final String VALID_PRIVATE_KEY = """
        -----BEGIN PRIVATE KEY-----
        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC7VJTUt9Us8cKj
        MzEfYyjiWA4R4/M2bS1+fWIcPm7tKEBLkLj6Y9pHJlYhBLJ3JkPvXgKqEPgLGGMl
        SKOXJi7eFGdNJcYmK3dqXl3hcr3U6v0yxvMqxSQLXZXqdxGTxMRdNVPq
        -----END PRIVATE KEY-----
        """;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tlsConfigService = new TlsConfigurationService(certificateValidator);
        ReflectionTestUtils.setField(tlsConfigService, "tempCertDir", tempDir.toString());
        ReflectionTestUtils.setField(tlsConfigService, "cleanupOnShutdown", true);
    }

    @AfterEach
    void tearDown() {
        // Use empty strings instead of null to avoid NPE in ConcurrentHashMap used by mockserver
        ConfigurationProperties.certificateAuthorityCertificate("");
        ConfigurationProperties.privateKeyPath("");
        ConfigurationProperties.tlsMutualAuthenticationCertificateChain("");
        ConfigurationProperties.tlsMutualAuthenticationRequired(false);
    }

    // Initialization Tests

    @Test
    @DisplayName("Should initialize successfully")
    void testInitialization() {
        assertDoesNotThrow(() -> tlsConfigService.init());
        assertTrue(Files.exists(tempDir));
    }

    // Certificate File Writing Tests

    @Test
    @DisplayName("Should write certificate to temp file successfully")
    void testWriteCertificateToTemp() throws IOException {
        String certPath = tlsConfigService.writeCertificateToTemp(
            "test-server",
            VALID_CERTIFICATE,
            "cert"
        );

        assertNotNull(certPath);
        Path certFile = Path.of(certPath);
        assertTrue(Files.exists(certFile));
        assertTrue(certFile.toString().contains("test-server"));
        assertTrue(certFile.toString().contains("cert"));

        String content = Files.readString(certFile);
        assertEquals(VALID_CERTIFICATE, content);
    }

    @Test
    @DisplayName("Should write multiple certificates for same server")
    void testWriteMultipleCertificatesForSameServer() throws IOException {
        String certPath = tlsConfigService.writeCertificateToTemp(
            "test-server",
            VALID_CERTIFICATE,
            "cert"
        );
        String keyPath = tlsConfigService.writeCertificateToTemp(
            "test-server",
            VALID_PRIVATE_KEY,
            "key"
        );

        assertNotNull(certPath);
        assertNotNull(keyPath);
        assertNotEquals(certPath, keyPath);
        assertEquals(2, tlsConfigService.getCertificateFileCount("test-server"));
    }

    @Test
    @DisplayName("Should track certificate file count correctly")
    void testCertificateFileCount() throws IOException {
        assertEquals(0, tlsConfigService.getCertificateFileCount("test-server"));

        tlsConfigService.writeCertificateToTemp("test-server", VALID_CERTIFICATE, "cert");
        assertEquals(1, tlsConfigService.getCertificateFileCount("test-server"));

        tlsConfigService.writeCertificateToTemp("test-server", VALID_PRIVATE_KEY, "key");
        assertEquals(2, tlsConfigService.getCertificateFileCount("test-server"));
    }

    @Test
    @DisplayName("Should reject empty certificate content")
    void testWriteEmptyCertificate() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> tlsConfigService.writeCertificateToTemp("test-server", "", "cert")
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject null certificate content")
    void testWriteNullCertificate() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> tlsConfigService.writeCertificateToTemp("test-server", null, "cert")
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    // TLS Configuration Tests

    @Test
    @DisplayName("Should configure TLS successfully")
    void testConfigureTls() throws IOException {
        TlsConfig tlsConfig = new TlsConfig();
        tlsConfig.setCertificate(VALID_CERTIFICATE);
        tlsConfig.setPrivateKey(VALID_PRIVATE_KEY);

        doNothing().when(certificateValidator).validateCertificateKeyPair(anyString(), anyString());

        assertDoesNotThrow(() -> tlsConfigService.configureTls("test-server", tlsConfig));
        assertEquals(2, tlsConfigService.getCertificateFileCount("test-server"));
    }

    @Test
    @DisplayName("Should configure TLS with mTLS")
    void testConfigureTlsWithMtls() throws IOException {
        MtlsConfig mtlsConfig = new MtlsConfig();
        mtlsConfig.setCaCertificate(VALID_CERTIFICATE);
        mtlsConfig.setRequireClientAuth(true);

        TlsConfig tlsConfig = new TlsConfig();
        tlsConfig.setCertificate(VALID_CERTIFICATE);
        tlsConfig.setPrivateKey(VALID_PRIVATE_KEY);
        tlsConfig.setMtlsConfig(mtlsConfig);

        doNothing().when(certificateValidator).validateCertificateKeyPair(anyString(), anyString());
        doNothing().when(certificateValidator).validateCaCertificate(anyString());

        assertDoesNotThrow(() -> tlsConfigService.configureTls("test-server", tlsConfig));
        assertEquals(3, tlsConfigService.getCertificateFileCount("test-server"));
    }

    @Test
    @DisplayName("Should propagate validation errors during TLS configuration")
    void testConfigureTlsWithInvalidCertificate() {
        TlsConfig tlsConfig = new TlsConfig();
        tlsConfig.setCertificate("invalid");
        tlsConfig.setPrivateKey(VALID_PRIVATE_KEY);

        doThrow(new InvalidCertificateException("Invalid certificate"))
            .when(certificateValidator).validateCertificateKeyPair(anyString(), anyString());

        assertThrows(
            InvalidCertificateException.class,
            () -> tlsConfigService.configureTls("test-server", tlsConfig)
        );
    }

    // Cleanup Tests

    @Test
    @DisplayName("Should cleanup server certificates successfully")
    void testCleanupServerCertificates() throws IOException {
        Path certFile = tempDir.resolve("test-server-cert.pem");
        Path keyFile = tempDir.resolve("test-server-key.pem");
        Files.writeString(certFile, VALID_CERTIFICATE);
        Files.writeString(keyFile, VALID_PRIVATE_KEY);

        tlsConfigService.writeCertificateToTemp("test-server", VALID_CERTIFICATE, "cert");
        tlsConfigService.writeCertificateToTemp("test-server", VALID_PRIVATE_KEY, "key");

        int countBefore = tlsConfigService.getCertificateFileCount("test-server");
        assertTrue(countBefore > 0);

        tlsConfigService.cleanupServerCertificates("test-server");

        assertEquals(0, tlsConfigService.getCertificateFileCount("test-server"));
    }

    @Test
    @DisplayName("Should handle cleanup of non-existent server")
    void testCleanupNonExistentServer() {
        assertDoesNotThrow(() -> tlsConfigService.cleanupServerCertificates("non-existent"));
    }

    @Test
    @DisplayName("Should cleanup all certificates on service cleanup")
    void testCleanupAllCertificates() throws IOException {
        tlsConfigService.writeCertificateToTemp("server1", VALID_CERTIFICATE, "cert");
        tlsConfigService.writeCertificateToTemp("server2", VALID_CERTIFICATE, "cert");

        assertEquals(1, tlsConfigService.getCertificateFileCount("server1"));
        assertEquals(1, tlsConfigService.getCertificateFileCount("server2"));

        tlsConfigService.cleanup();

        assertEquals(0, tlsConfigService.getCertificateFileCount("server1"));
        assertEquals(0, tlsConfigService.getCertificateFileCount("server2"));
    }

    @Test
    @DisplayName("Should not cleanup when disabled")
    void testCleanupDisabled() throws IOException {
        ReflectionTestUtils.setField(tlsConfigService, "cleanupOnShutdown", false);

        tlsConfigService.writeCertificateToTemp("test-server", VALID_CERTIFICATE, "cert");
        assertEquals(1, tlsConfigService.getCertificateFileCount("test-server"));

        tlsConfigService.cleanup();

        // Should still have the certificate since cleanup is disabled
        assertEquals(1, tlsConfigService.getCertificateFileCount("test-server"));
    }

    @Test
    @DisplayName("Should delete actual certificate files during cleanup")
    void testActualFileDeletion() throws IOException {
        String certPath = tlsConfigService.writeCertificateToTemp(
            "test-server",
            VALID_CERTIFICATE,
            "cert"
        );

        Path certFile = Path.of(certPath);
        assertTrue(Files.exists(certFile));

        tlsConfigService.cleanupServerCertificates("test-server");

        assertFalse(Files.exists(certFile));
    }
}
