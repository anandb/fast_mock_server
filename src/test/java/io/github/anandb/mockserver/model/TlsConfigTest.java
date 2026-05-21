package io.github.anandb.mockserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TlsConfig Tests")
class TlsConfigTest {

    @Test
    void isValidReturnsTrueWhenBothPresent() {
        TlsConfig config = new TlsConfig("cert", "key", null);
        assertTrue(config.isValid());
    }

    @Test
    void isValidReturnsTrueWhenBothPresentWithSpaces() {
        TlsConfig config = new TlsConfig("  cert  ", "  key  ", null);
        assertTrue(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenCertificateNull() {
        TlsConfig config = new TlsConfig(null, "key", null);
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenPrivateKeyNull() {
        TlsConfig config = new TlsConfig("cert", null, null);
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenCertificateEmpty() {
        TlsConfig config = new TlsConfig("", "key", null);
        assertFalse(config.isValid());
    }

    @Test
    void hasMtlsReturnsFalseWhenNull() {
        TlsConfig config = new TlsConfig("cert", "key", null);
        assertFalse(config.hasMtls());
    }

    @Test
    void hasMtlsReturnsTrueWhenValid() {
        MtlsConfig mtlsConfig = new MtlsConfig();
        mtlsConfig.setCaCertificate("ca-cert");
        mtlsConfig.setRequireClientAuth(true);
        TlsConfig config = new TlsConfig("cert", "key", mtlsConfig);
        assertTrue(config.hasMtls());
    }

    @Test
    void hasMtlsReturnsFalseWithInvalidMtls() {
        MtlsConfig mtlsConfig = new MtlsConfig();
        mtlsConfig.setCaCertificate("  ");
        mtlsConfig.setRequireClientAuth(false);
        TlsConfig config = new TlsConfig("cert", "key", mtlsConfig);
        assertFalse(config.hasMtls());
    }

    @Test
    void stripSpacesRemovesWhitespace() {
        TlsConfig config = new TlsConfig(" c e r t ", " k e y ", null);
        config.stripSpaces();
        assertEquals("cert", config.getCertificate());
        assertEquals("key", config.getPrivateKey());
    }

    @Test
    void stripSpacesAlsoStripsMtls() {
        MtlsConfig mtlsConfig = new MtlsConfig();
        mtlsConfig.setCaCertificate(" c a ");
        mtlsConfig.setRequireClientAuth(true);
        TlsConfig config = new TlsConfig("c e r t", " k e y ", mtlsConfig);
        config.stripSpaces();
        assertEquals("cert", config.getCertificate());
        assertEquals("key", config.getPrivateKey());
        assertEquals("ca", config.getMtlsConfig().getCaCertificate());
    }
}
