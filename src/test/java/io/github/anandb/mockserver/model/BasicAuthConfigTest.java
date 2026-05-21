package io.github.anandb.mockserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BasicAuthConfig Tests")
class BasicAuthConfigTest {

    @Test
    void isValidReturnsTrueWhenBothPresent() {
        BasicAuthConfig config = new BasicAuthConfig("user", "pass");
        assertTrue(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenUsernameNull() {
        BasicAuthConfig config = new BasicAuthConfig(null, "pass");
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenPasswordNull() {
        BasicAuthConfig config = new BasicAuthConfig("user", null);
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenUsernameBlank() {
        BasicAuthConfig config = new BasicAuthConfig("   ", "pass");
        assertFalse(config.isValid());
    }

    @Test
    void isValidReturnsFalseWhenPasswordBlank() {
        BasicAuthConfig config = new BasicAuthConfig("user", "   ");
        assertFalse(config.isValid());
    }

    @Test
    void toStringExcludesPassword() {
        BasicAuthConfig config = new BasicAuthConfig("myuser", "mypassword");
        String str = config.toString();
        assertTrue(str.contains("myuser"));
        assertFalse(str.contains("mypassword"));
    }
}
