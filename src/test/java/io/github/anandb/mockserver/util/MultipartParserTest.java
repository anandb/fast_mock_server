package io.github.anandb.mockserver.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MultipartParser Tests")
class MultipartParserTest {

    private static final String BOUNDARY = "----TestBoundary123";

    @Test
    void parseEmptyBodyReturnsEmptyList() {
        List<MultipartParser.Part> parts = MultipartParser.parse(new byte[0], "multipart/form-data; boundary=" + BOUNDARY);
        assertTrue(parts.isEmpty());
    }

    @Test
    void parseNullBodyReturnsEmptyList() {
        List<MultipartParser.Part> parts = MultipartParser.parse(null, "multipart/form-data; boundary=" + BOUNDARY);
        assertTrue(parts.isEmpty());
    }

    @Test
    void parseNullContentTypeThrows() {
        byte[] body = "--boundary\r\n\r\nContent-Disposition: form-data; name=\"field\"\r\n\r\nvalue\r\n--boundary--\r\n"
            .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.parse(body, null));
    }

    @Test
    void parseBlankContentTypeThrows() {
        byte[] body = "--boundary\r\n\r\nContent-Disposition: form-data; name=\"field\"\r\n\r\nvalue\r\n--boundary--\r\n"
            .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.parse(body, "  "));
    }

    @Test
    void parseMissingBoundaryInContentTypeThrows() {
        byte[] body = "--boundary\r\n\r\nContent-Disposition: form-data; name=\"field\"\r\n\r\nvalue\r\n--boundary--\r\n"
            .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.parse(body, "multipart/form-data"));
    }

    @Test
    void parseSimpleTextField() {
        String bodyStr = "--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"username\"\r\n" +
            "\r\n" +
            "john_doe\r\n" +
            "--" + BOUNDARY + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        List<MultipartParser.Part> parts = MultipartParser.parse(body, "multipart/form-data; boundary=" + BOUNDARY);

        assertEquals(1, parts.size());
        assertEquals("username", parts.get(0).getFieldName());
        assertNull(parts.get(0).getFileName());
        assertEquals("john_doe", new String(parts.get(0).getContent(), StandardCharsets.UTF_8));
    }

    @Test
    void parseFileUploadPart() {
        String bodyStr = "--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "Hello World\r\n" +
            "--" + BOUNDARY + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        List<MultipartParser.Part> parts = MultipartParser.parse(body, "multipart/form-data; boundary=" + BOUNDARY);

        assertEquals(1, parts.size());
        assertEquals("file", parts.get(0).getFieldName());
        assertEquals("test.txt", parts.get(0).getFileName());
        assertEquals("text/plain", parts.get(0).getContentType());
        assertEquals("Hello World", new String(parts.get(0).getContent(), StandardCharsets.UTF_8));
    }

    @Test
    void parseMultipleParts() {
        String bodyStr = "--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"field1\"\r\n" +
            "\r\n" +
            "value1\r\n" +
            "--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"field2\"\r\n" +
            "\r\n" +
            "value2\r\n" +
            "--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"upload\"; filename=\"data.bin\"\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "\r\n" +
            "binary-data\r\n" +
            "--" + BOUNDARY + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        List<MultipartParser.Part> parts = MultipartParser.parse(body, "multipart/form-data; boundary=" + BOUNDARY);

        assertEquals(3, parts.size());
        assertEquals("field1", parts.get(0).getFieldName());
        assertEquals("field2", parts.get(1).getFieldName());
        assertEquals("upload", parts.get(2).getFieldName());
        assertEquals("data.bin", parts.get(2).getFileName());
    }

    @Test
    void parseRejectsPathTraversalInFileName() {
        String bodyStr = "--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"../../etc/passwd\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "malicious\r\n" +
            "--" + BOUNDARY + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.parse(body, "multipart/form-data; boundary=" + BOUNDARY));
    }

    @Test
    void parseRejectsBackslashInFileName() {
        String bodyStr = "--" + BOUNDARY + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"..\\\\secret.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "content\r\n" +
            "--" + BOUNDARY + "--\r\n";
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.parse(body, "multipart/form-data; boundary=" + BOUNDARY));
    }

    @Test
    void extractBoundaryFromContentType() {
        assertEquals("abc123", MultipartParser.extractBoundary("multipart/form-data; boundary=abc123"));
    }

    @Test
    void extractBoundaryWithQuotes() {
        assertEquals("abc123", MultipartParser.extractBoundary("multipart/form-data; boundary=\"abc123\""));
    }

    @Test
    void extractBoundaryWithSemicolonFollowing() {
        assertEquals("abc123", MultipartParser.extractBoundary("multipart/form-data; boundary=abc123; charset=UTF-8"));
    }

    @Test
    void extractBoundaryReturnsNullWhenMissing() {
        assertNull(MultipartParser.extractBoundary("multipart/form-data"));
    }

    @Test
    void validateFileNameRejectsDoubleDots() {
        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.validateFileName("file..name"));
    }

    @Test
    void validateFileNameRejectsForwardSlash() {
        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.validateFileName("path/to/file.txt"));
    }

    @Test
    void validateFileNameRejectsBackslash() {
        assertThrows(IllegalArgumentException.class,
            () -> MultipartParser.validateFileName("path\\to\\file.txt"));
    }

    @Test
    void validateFileNameAcceptsValidName() {
        assertDoesNotThrow(() -> MultipartParser.validateFileName("normal-file.txt"));
    }
}
