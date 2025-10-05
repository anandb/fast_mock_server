package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.exception.InvalidCertificateException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for CertificateValidator.
 * <p>
 * Tests validation logic for certificates, private keys, and CA certificates
 * in various formats and conditions.
 * </p>
 */
@DisplayName("CertificateValidator Tests")
class CertificateValidatorTest {

    private CertificateValidator validator;

    // Valid test certificate (self-signed, for testing only)
    // Generated with: openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 -nodes
    private static final String VALID_CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIDazCCAlOgAwIBAgIUI3B0wIAeNe8WRPL28Q5HRJwPamkwDQYJKoZIhvcNAQEL
        BQAwRTELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWExDTALBgNVBAoM
        BFRlc3QxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0yNTEwMDIxNzEwMzRaFw0yNjEw
        MDIxNzEwMzRaMEUxCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMQ0w
        CwYDVQQKDARUZXN0MRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEB
        AQUAA4IBDwAwggEKAoIBAQC2Ovj7OiUyMCWXN42W+jXmMaoILFH2MtlySrRrJf9i
        ID/mO4Ry/KegrXWbX6rvzr50zLKj0COKdZCA8IKJj3jZUOXoQ3jiZCzkpm5OQSGk
        OA6b8PqIeRDy9CughEw469WbAwjajJ1ikwNwmFpvfHt04u8Q3t35PhOpulE+kLhg
        YQFP1+9GVMq/n1g4C8SlaUfvDhMaRin/QOYB6LGKUB9FLYM1EHaUhEcLjXzefeS7
        sLb6ipEVZX4hcBwLcfceVm9Y7SLrgfScAtFk+SoapEQTI56cMu+AS9YLkngnJtFn
        u9DOo5+2I90QRgmoEudIv8B9HE5k0eGNxSWVLdzLsONJAgMBAAGjUzBRMB0GA1Ud
        DgQWBBS81AXurOkHCCdDgCW5hTzgTg3IxDAfBgNVHSMEGDAWgBS81AXurOkHCCdD
        gCW5hTzgTg3IxDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBL
        9sCN5fNtW/iIUW6hVJN59zApqt2CP8qo4d7Y3f0O7TMFmYQiJo5OTCtC8Dazlu5Y
        pD37PUp5RXWp7FeUVlK+oeUWa2ptiwPKt3+8RxvcKrmgwOKV+bpEFdezwp5j5Lb0
        BQZwPOqjC2FY26iwKEqjG/gqAbD4c+FuNzgpETUYq8G1EAk5kFoxTDVqy0HhsR9i
        FZN6x5qVoPF0avVtqJVfGT6XxKW1zKVi6jDXQmaRHhVGKlBeryTHUqaZdlXd4Cpw
        b37IvyKFyhBFHJF1wRMk1/QFq+OtC/YO/lu/DUI81EgdQBGa46mQUqZQVOFGXkcJ
        j9Ld7jjf6R7LGC+5NbcU
        -----END CERTIFICATE-----
        """;

    private static final String VALID_PRIVATE_KEY = """
        -----BEGIN PRIVATE KEY-----
        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC2Ovj7OiUyMCWX
        N42W+jXmMaoILFH2MtlySrRrJf9iID/mO4Ry/KegrXWbX6rvzr50zLKj0COKdZCA
        8IKJj3jZUOXoQ3jiZCzkpm5OQSGkOA6b8PqIeRDy9CughEw469WbAwjajJ1ikwNw
        mFpvfHt04u8Q3t35PhOpulE+kLhgYQFP1+9GVMq/n1g4C8SlaUfvDhMaRin/QOYB
        6LGKUB9FLYM1EHaUhEcLjXzefeS7sLb6ipEVZX4hcBwLcfceVm9Y7SLrgfScAtFk
        +SoapEQTI56cMu+AS9YLkngnJtFnu9DOo5+2I90QRgmoEudIv8B9HE5k0eGNxSWV
        LdzLsONJAgMBAAECggEAKKB/L+tnrYfEabEt9PH1oEuEP+w5naz6g5uaG6VOCX9t
        H5Q3VDd128GwTaKHho1OMtYMIa9erRjGzahRQf/ljnWVvDGGutpgk91zDClDKcyB
        nUydXl23ZKMBMvkr0Sjq53NGvwrIb9ic1LZyuxZCo+2QJO+7Bz1KvvVV9Z/oKzw0
        MfMLeJFW7IGR/I1SPGtFasaV11OLWoVwLVzkwwntLCZMkhHLbK/QDYrLlrlHG+h5
        BJB1mL7O2hgWvM7pGROZho5kNK8MsvPWPLdRfAuv2sZvsLRxdNLhFzcDymq/j3At
        /amtOU0QaUXW23GCaDHtAjlJZG3MGE7x1mLPT1ACzQKBgQD0warHuEDRVKw3/+w5
        UANzRTuoj1kZ6uOfYnz8dtR8rw2JYhoiLOKEmukc/QUuvc7UpcpC3Xu5IyZrIc7p
        VaJXGynkDI3qDlOuJ6UNFBXM6AXPeJMogiONsgMuJ0OquT3v/1BlD6b0EgCiAAo1
        /i+Zs5UMcBVPBGFgWVkj0wJ6QwKBgQC+mf+3DyQwro4FUoEoYRfLe0S67kf3tOVV
        AtD+iwRZHo2TLehW2W1J3xjwk/X0SxZg4opocZ7ST58gaxspiZ2R0SsvQiKs8D+G
        eBWcv2aJatFfylog2oN/qYGj24ERlT+Z0T2grQAuzwetDWMEIs1QXK60SSrOPx4I
        vIv2hUyxgwKBgQCWqo1Nqn/EHKJzSoiI81dcYxcJeUy5Jp3+ZGtaInBFXKExnAFI
        XmcGiDHOFXdWQaOLxY8PwDXETv38XvB3NK7dfiw4ZP4LQcyDXYY92cpdu8qv36J/
        AjWOqTFJ/QOrmaKmFX3Q6GS2xEo9b1bZy+JTdHfhEzIa1TB4VigIZIGW7wKBgCRe
        TB3I5NXpKK45TO3AgQLRlY65kr/gI3YyIGDBc+XxX4A63T9dI25aBG87PE4N2cpI
        z7IYI/7rNGjCJx+o44kESWIuE2G+QdDNrwoGt3D3EdZeTQ5Bw5+jX1o9swYA3W7j
        Lwgumn9T/5n0mPUcEuMHnEpIP8O05W0zVN2IF9czAoGBAKGHUUJkFeUEyMTX8X/i
        Pt5N0rP2X1hPuIiHIkOy+1yWmoW8iM2pwMYd42CRLtlBTyHtcbxWU8OnCZN5FeIg
        G+V1pI/wtPjOIGY5+P0UJtbVYz1HAHrfEBY39kBKMUb+eX03oWP5nVC+F8a6Gcvb
        SCk7pHONR7Oyrd72TqUMLU9F
        -----END PRIVATE KEY-----
        """;

    private static final String VALID_RSA_PRIVATE_KEY = """
        -----BEGIN RSA PRIVATE KEY-----
        MIIEpAIBAAKCAQEAtjr4+zolMjAllzeNlvo15jGqCCxR9jLZckq0ayX/YiA/5juE
        cvynoK11m1+q7866dMyyo9AjinWQgPCCiY942VDl6EN44mQs5KZuTkEhpDgOm/D6
        iHkQ8vQroIRMOOvVmwMI2oydYpMDcJhab3x7dOLvEN7d+T4TqbpRPpC4YGE

BT9fvR
        lTKv59YOAvEpWlH7w4TGkYp/0DmAeixilAfRS2DNRB2lIRHC4183n3ku7C2+oqRF
        WV+IXAcC3H3HlZvWO0i64H0nALRZPkqGqREEyOenDLvgEvWC5J4JybRZ7vQzqOft
        iPdEEYJqBLnSL/AfRxOZNHhjcUllS3cy7DjSQIDAQABAoIBACigfy/rZ62HxGmxL
        fTx9aBLhD/sOZ2s+oObmhulTgl/bR+UN1Q3ddvBsE2ih4aNTjLWDCGvXq0Yxs2oU
        UH/5Y51lbwxhrrYJPdcwwopQynMgZ1MnV5dt2SjATL5K9Eo6udzRr8KyG/YnNS2c
        rsWQqPtkCTvuwc9Sr71VfWf6Cs8NDHzC3iRVuyBkfyNUjxrRWrGlddTi1qFcC1c5
        MMJ7SwmTJIRy2yv0A2Ky5a5RxvoeQSQdZi+ztoYFrzO6RkTmYaOZDSvDLLz1jy3U
        XwLr9rGb7C0cXTS4Rc3A8pqv49wLf2prTlNEGlF1ttxgmgx7QI5SWRtzBhO8dZiz
        09QAs0CgYEA9MGqx7hA0VSsN//sOVADc0U7qI9ZGerjn2J8/HbUfK8NiWIaIizih
        JrpHP0FLr3O1KXKQt17uSMmayHO6VWiVxsp5AyN6g5TriejjRQVzOgFz3iTKIIjj
        bIDLidDqrk97/9QZQ+m9BIAogAKNf4vmbOVDHAVTwRhYFlZI9MCekMCgYEAvpn/t
        w8kMK6OBVKBKGEXy3tEuu5H97TlVQLQ/osEWR6Nky3oVtltSd8Y8JP19EsWYOKKa
        HGe0k+fIGsbKYmdkdErL0IirPA/hngVnL9miWrRX8paINqDf6mBo9uBEZU/mdE9o
        K0ALs8HrQ1jBCLNUFyutEkqzj8eCLyL9oVMsYMCgYEAlqqNTap/xByic0qIiPNXX
        GMXCXlMuSad/mRrWiJwRVyhMZwBSF5nBogxzhV3VkGji8WPD8A1xE79/F7wdzSu3
        X4sOGT+C0HMg12GPdnKXbvKr9+ifwI1jqkxSf0Dq5miphV90OhktsRKPW9W2cviU
        3R34RMyGtUweF YoCGSBlu8CgYAkXkwdyOTV6SiuOUztwIEC0ZWOuZK/4CN2MiBg
        wXPl8V+AOt0/XSNuWgRvOzxODdnKSM+yGCP+6zRowicfqOOJBEliLhNhvkHQza8K
        Brdw9xHWXk0OQcOfo19aPbMGAN1u4y8ILpp/U/+Z9Jj1HBLjB5xKSD/DtOVtM1Td
        iBfXMwKBgQChh1FCZBXlBMjE1/F/4j7eTdKz9l9YT7iIhyJDsvtclpqFvIjNqcDG
        HeNgkS7ZQU8h7XG8VlPDpwmTeRXiIBvldaSP8LT4ziBmOfj9FCbW1WM9RwB63xAW
        N/ZASjFG/nl9N6Fj+Z1QvhfGuhnL20gpO6RzjUezssq3e9k6lCxPRUQ==
        -----END RSA PRIVATE KEY-----
        """;

    private static final String VALID_EC_PRIVATE_KEY = """
        -----BEGIN EC PRIVATE KEY-----
        MHcCAQEEIIGlRHnFqjfAHJT3X8Y9R3t7VQqH5jLMEFdGJWKHwCJDoAoGCCqGSM49
        AwEHoUQDQgAEtjr4+zolMjAllzeNlvo15jGqCCxR9jLZckq0ayX/YiA/5juEcvyn
        oK11m1+q7866dMyyo9AjinWQgPCCiY942VDl6EN44mQs5KZuTkEhpDgOm/D6iHkQ
        8vQroIRMOOvVmwMI2oydYpMDcJhab3x7dOLvEN7d+T4TqbpRPpC4YGEGT9fvRlTK
        v59YOAvEpWlH7w4TGkYp/0DmAeixilAfRS2DNRB2lIRHC4183n3ku7C2+oqRFWV+
        IXAcC3H3HlZvWO0i64H0nALRZPkqGqREEyOenDLvgEvWC5J4JybRZ7vQzqOftiPd
        EEYJqBLnSL/AfRxOZNHhjcUllS3cy7DjSQ==
        -----END EC PRIVATE KEY-----
        """;

    @BeforeEach
    void setUp() {
        validator = new CertificateValidator();
    }

    // Certificate Format Validation Tests

    @Test
    @DisplayName("Should accept valid certificate format")
    void testValidCertificateFormat() {
        assertDoesNotThrow(() -> validator.validateCertificateFormat(VALID_CERTIFICATE));
    }

    @Test
    @DisplayName("Should reject null certificate")
    void testNullCertificate() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateFormat(null)
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject empty certificate")
    void testEmptyCertificate() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateFormat("")
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject certificate without BEGIN marker")
    void testCertificateWithoutBeginMarker() {
        String invalidCert = "Invalid certificate content\n-----END CERTIFICATE-----";
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateFormat(invalidCert)
        );
        assertTrue(exception.getMessage().contains("PEM format"));
    }

    @Test
    @DisplayName("Should reject certificate without END marker")
    void testCertificateWithoutEndMarker() {
        String invalidCert = "-----BEGIN CERTIFICATE-----\nInvalid certificate content";
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateFormat(invalidCert)
        );
        assertTrue(exception.getMessage().contains("PEM format"));
    }

    @Test
    @DisplayName("Should reject certificate with invalid content")
    void testCertificateWithInvalidContent() {
        String invalidCert = "-----BEGIN CERTIFICATE-----\nInvalidBase64Content!!!\n-----END CERTIFICATE-----";
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateFormat(invalidCert)
        );
        assertTrue(exception.getMessage().contains("Invalid certificate"));
    }

    // Private Key Format Validation Tests

    @Test
    @DisplayName("Should accept valid PRIVATE KEY format")
    void testValidPrivateKey() {
        assertDoesNotThrow(() -> validator.validatePrivateKeyFormat(VALID_PRIVATE_KEY));
    }

    @Test
    @DisplayName("Should accept valid RSA PRIVATE KEY format")
    void testValidRsaPrivateKey() {
        assertDoesNotThrow(() -> validator.validatePrivateKeyFormat(VALID_RSA_PRIVATE_KEY));
    }

    @Test
    @DisplayName("Should accept valid EC PRIVATE KEY format")
    void testValidEcPrivateKey() {
        assertDoesNotThrow(() -> validator.validatePrivateKeyFormat(VALID_EC_PRIVATE_KEY));
    }

    @Test
    @DisplayName("Should reject null private key")
    void testNullPrivateKey() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validatePrivateKeyFormat(null)
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject empty private key")
    void testEmptyPrivateKey() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validatePrivateKeyFormat("")
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject private key without BEGIN marker")
    void testPrivateKeyWithoutBeginMarker() {
        String invalidKey = "Invalid key content\n-----END PRIVATE KEY-----";
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validatePrivateKeyFormat(invalidKey)
        );
        assertTrue(exception.getMessage().contains("PEM format"));
    }

    @Test
    @DisplayName("Should reject private key without END marker")
    void testPrivateKeyWithoutEndMarker() {
        String invalidKey = "-----BEGIN PRIVATE KEY-----\nInvalid key content";
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validatePrivateKeyFormat(invalidKey)
        );
        assertTrue(exception.getMessage().contains("PEM format"));
    }

    @Test
    @DisplayName("Should reject unrecognized private key type")
    void testUnrecognizedPrivateKeyType() {
        String invalidKey = "-----BEGIN UNKNOWN KEY-----\nContent\n-----END UNKNOWN KEY-----";
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validatePrivateKeyFormat(invalidKey)
        );
        assertTrue(exception.getMessage().contains("Unrecognized private key format"));
    }

    // CA Certificate Validation Tests

    @Test
    @DisplayName("Should accept valid CA certificate")
    void testValidCaCertificate() {
        assertDoesNotThrow(() -> validator.validateCaCertificate(VALID_CERTIFICATE));
    }

    @Test
    @DisplayName("Should reject null CA certificate")
    void testNullCaCertificate() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCaCertificate(null)
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject empty CA certificate")
    void testEmptyCaCertificate() {
        InvalidCertificateException exception = assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCaCertificate("")
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    // Certificate-Key Pair Validation Tests

    @Test
    @DisplayName("Should accept valid certificate-key pair")
    void testValidCertificateKeyPair() {
        assertDoesNotThrow(() ->
            validator.validateCertificateKeyPair(VALID_CERTIFICATE, VALID_PRIVATE_KEY)
        );
    }

    @Test
    @DisplayName("Should reject certificate-key pair with invalid certificate")
    void testCertificateKeyPairWithInvalidCertificate() {
        assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateKeyPair("invalid", VALID_PRIVATE_KEY)
        );
    }

    @Test
    @DisplayName("Should reject certificate-key pair with invalid private key")
    void testCertificateKeyPairWithInvalidKey() {
        assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateKeyPair(VALID_CERTIFICATE, "invalid")
        );
    }

    @Test
    @DisplayName("Should reject certificate-key pair with both invalid")
    void testCertificateKeyPairBothInvalid() {
        assertThrows(
            InvalidCertificateException.class,
            () -> validator.validateCertificateKeyPair("invalid", "invalid")
        );
    }
}
