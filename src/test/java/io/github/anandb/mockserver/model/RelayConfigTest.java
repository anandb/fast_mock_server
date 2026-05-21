package io.github.anandb.mockserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RelayConfig Tests")
class RelayConfigTest {

    @Test
    void getAllPrefixesReturnsDefaultWhenNull() {
        RelayConfig config = new RelayConfig();
        config.setPrefixes(null);
        assertEquals(List.of("/**"), config.getAllPrefixes());
    }

    @Test
    void getAllPrefixesReturnsDefaultWhenEmpty() {
        RelayConfig config = new RelayConfig();
        config.setPrefixes(List.of());
        assertEquals(List.of("/**"), config.getAllPrefixes());
    }

    @Test
    void getAllPrefixesReturnsConfigured() {
        RelayConfig config = new RelayConfig();
        config.setPrefixes(List.of("/api/**", "/health"));
        assertEquals(List.of("/api/**", "/health"), config.getAllPrefixes());
    }

    @Test
    void isOAuth2EnabledReturnsTrueWhenAllPresent() {
        RelayConfig config = new RelayConfig();
        config.setTokenUrl("https://token.example.com");
        config.setClientId("client");
        config.setClientSecret("secret");
        assertTrue(config.isOAuth2Enabled());
    }

    @Test
    void isOAuth2EnabledReturnsFalseWhenTokenUrlNull() {
        RelayConfig config = new RelayConfig();
        config.setClientId("client");
        config.setClientSecret("secret");
        assertFalse(config.isOAuth2Enabled());
    }

    @Test
    void isOAuth2EnabledReturnsFalseWhenClientIdNull() {
        RelayConfig config = new RelayConfig();
        config.setTokenUrl("https://token.example.com");
        config.setClientSecret("secret");
        assertFalse(config.isOAuth2Enabled());
    }

    @Test
    void isOAuth2EnabledReturnsFalseWhenClientSecretNull() {
        RelayConfig config = new RelayConfig();
        config.setTokenUrl("https://token.example.com");
        config.setClientId("client");
        assertFalse(config.isOAuth2Enabled());
    }

    @Test
    void isTunnelEnabledReturnsFalseWhenNull() {
        RelayConfig config = new RelayConfig();
        assertFalse(config.isTunnelEnabled());
    }

    @Test
    void isTunnelEnabledReturnsTrueWhenValid() {
        RelayConfig config = new RelayConfig();
        config.setTunnelConfig(new TunnelConfig("ns", "pod", 8080));
        assertTrue(config.isTunnelEnabled());
    }

    @Test
    void isValidReturnsTrueWithRemoteUrl() {
        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("https://api.example.com");
        assertTrue(config.isValid());
    }

    @Test
    void isValidReturnsFalseWithoutRemoteUrlOrTunnel() {
        RelayConfig config = new RelayConfig();
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsTrueWithTunnelConfig() {
        RelayConfig config = new RelayConfig();
        config.setTunnelConfig(new TunnelConfig("ns", "pod", 8080));
        assertTrue(config.isValid());
    }

    @Test
    void isValidReturnsFalseWithPartialOAuth2() {
        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("https://api.example.com");
        config.setTokenUrl("https://token.example.com");
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsTrueWithCompleteOAuth2() {
        RelayConfig config = new RelayConfig();
        config.setRemoteUrl("https://api.example.com");
        config.setTokenUrl("https://token.example.com");
        config.setClientId("client");
        config.setClientSecret("secret");
        assertTrue(config.isValid());
    }

    @Test
    void hasHeadersReturnsTrueWhenPresent() {
        RelayConfig config = new RelayConfig();
        config.setHeaders(Map.of("X-Forward", "true"));
        assertTrue(config.hasHeaders());
    }

    @Test
    void hasHeadersReturnsFalseWhenNull() {
        RelayConfig config = new RelayConfig();
        assertFalse(config.hasHeaders());
    }

    @Test
    void hasHeadersReturnsFalseWhenEmpty() {
        RelayConfig config = new RelayConfig();
        config.setHeaders(Map.of());
        assertFalse(config.hasHeaders());
    }

    @Test
    void defaultGrantTypeIsClientCredentials() {
        RelayConfig config = new RelayConfig();
        assertEquals("client_credentials", config.getGrantType());
    }
}
