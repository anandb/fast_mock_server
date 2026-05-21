package io.github.anandb.mockserver.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Exception Tests")
class CustomExceptionTest {

    @Test
    void invalidCertificateExceptionMessage() {
        InvalidCertificateException ex = new InvalidCertificateException("bad cert");
        assertEquals("bad cert", ex.getMessage());
    }

    @Test
    void invalidCertificateExceptionWithCause() {
        Throwable cause = new RuntimeException("root");
        InvalidCertificateException ex = new InvalidCertificateException("bad cert", cause);
        assertEquals("bad cert", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void invalidExpectationExceptionMessage() {
        InvalidExpectationException ex = new InvalidExpectationException("invalid json");
        assertEquals("invalid json", ex.getMessage());
    }

    @Test
    void invalidExpectationExceptionWithCause() {
        Throwable cause = new RuntimeException("parse error");
        InvalidExpectationException ex = new InvalidExpectationException("invalid json", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void serverNotFoundExceptionIncludesId() {
        ServerNotFoundException ex = new ServerNotFoundException("my-server");
        assertTrue(ex.getMessage().contains("my-server"));
    }

    @Test
    void serverNotFoundExceptionWithCause() {
        Throwable cause = new RuntimeException("reason");
        ServerNotFoundException ex = new ServerNotFoundException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void serverCreationExceptionMessage() {
        ServerCreationException ex = new ServerCreationException("creation failed");
        assertEquals("creation failed", ex.getMessage());
    }

    @Test
    void serverCreationExceptionWithCause() {
        Throwable cause = new RuntimeException("port in use");
        ServerCreationException ex = new ServerCreationException("creation failed", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void serverAlreadyExistsExceptionDefaultMessage() {
        ServerAlreadyExistsException ex = new ServerAlreadyExistsException("dup-server");
        assertTrue(ex.getMessage().contains("dup-server"));
    }

    @Test
    void serverAlreadyExistsExceptionWithCause() {
        Throwable cause = new RuntimeException("conflict");
        ServerAlreadyExistsException ex = new ServerAlreadyExistsException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
