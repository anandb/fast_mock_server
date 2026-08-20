package io.github.anandb.mockserver.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for multipart/form-data request bodies.
 * <p>
 * Extracts individual parts (fields and files) from a raw multipart body.
 * Validates file names against path traversal attacks ({@code ..}, {@code /}, {@code \}).
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MultipartParser {

    /**
     * A single parsed part from a multipart body.
     */
    @Data
    public static class Part {
        /** The form field name (from Content-Disposition name=). */
        private final String fieldName;
        /** The original file name (from Content-Disposition filename=), null for non-file parts. */
        private final String fileName;
        /** The Content-Type of this part, null if not specified. */
        private final String contentType;
        /** The raw bytes of this part's content. */
        private final byte[] content;
    }

    /**
     * Parses a multipart/form-data body.
     *
     * @param body the raw request body bytes
     * @param contentType the Content-Type header value (must contain boundary=)
     * @return list of parsed parts
     * @throws IllegalArgumentException if the body is empty, boundary is missing, or a file name contains path traversal
     */
    public static List<Part> parse(byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            return List.of();
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content-Type header is required for multipart parsing");
        }

        String boundary = extractBoundary(contentType);
        if (boundary == null || boundary.isBlank()) {
            throw new IllegalArgumentException("No boundary found in Content-Type: " + contentType);
        }

        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] closingBoundary = (boundaryBytes.length + 2 >= body.length) ? new byte[0] : null;

        List<Part> parts = new ArrayList<>();
        int pos = 0;

        // Find first boundary
        int firstBoundary = indexOf(body, boundaryBytes, 0);
        if (firstBoundary == -1) {
            throw new IllegalArgumentException("Boundary not found in multipart body");
        }
        pos = firstBoundary + boundaryBytes.length;

        while (pos < body.length) {
            // Skip CRLF after boundary
            pos = skipCrlf(body, pos);
            if (pos >= body.length) {
                break;
            }

            // Check for closing boundary (--boundary--)
            if (pos + 2 < body.length && body[pos] == '-' && body[pos + 1] == '-') {
                break;
            }

            // Parse part headers
            int headerEnd = findHeaderEnd(body, pos);
            if (headerEnd == -1) {
                break;
            }

            String headers = new String(body, pos, headerEnd - pos, StandardCharsets.UTF_8);
            String fieldName = extractHeaderValue(headers, "name");
            String fileName = extractHeaderValue(headers, "filename");
            String partContentType = extractContentType(headers);

            // Skip past header end (CRLF CRLF)
            pos = headerEnd + 4;

            // Find next boundary
            int nextBoundary = indexOf(body, boundaryBytes, pos);
            if (nextBoundary == -1) {
                break;
            }

            // Content is between pos and nextBoundary, minus trailing CRLF
            int contentEnd = nextBoundary - 2; // -2 for CRLF before boundary
            if (contentEnd < pos) {
                contentEnd = pos;
            }
            byte[] content = new byte[contentEnd - pos];
            System.arraycopy(body, pos, content, 0, content.length);

            // Validate file name against path traversal
            if (fileName != null) {
                validateFileName(fileName);
            }

            parts.add(new Part(fieldName, fileName, partContentType, content));
            pos = nextBoundary + boundaryBytes.length;
        }

        return parts;
    }

    /**
     * Extracts the boundary string from a Content-Type header value.
     */
    static String extractBoundary(String contentType) {
        int idx = contentType.indexOf("boundary=");
        if (idx == -1) {
            return null;
        }
        String boundary = contentType.substring(idx + 9).trim();
        // Remove surrounding quotes if present
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        // Stop at semicolon if other params follow
        int semi = boundary.indexOf(';');
        if (semi != -1) {
            boundary = boundary.substring(0, semi).trim();
        }
        return boundary;
    }

    /**
     * Validates a file name does not contain path traversal characters.
     *
     * @throws IllegalArgumentException if the file name contains {@code ..}, {@code /}, or {@code \}
     */
    static void validateFileName(String fileName) {
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("Path traversal rejected: file name contains '..': " + fileName);
        }
        if (fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Path traversal rejected: file name contains path separator: " + fileName);
        }
    }

    private static int skipCrlf(byte[] data, int pos) {
        while (pos < data.length && (data[pos] == '\r' || data[pos] == '\n')) {
            pos++;
        }
        return pos;
    }

    private static int findHeaderEnd(byte[] data, int start) {
        // Look for \r\n\r\n
        for (int i = start; i + 3 < data.length; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private static String extractHeaderValue(String headers, String headerName) {
        // Search for Content-Disposition with the given attribute.
        // Use word-boundary check: attribute must be preceded by ';' or start of headers
        // to avoid matching "name" inside "filename".
        String lower = headers.toLowerCase();
        String attrLower = headerName.toLowerCase();
        int searchFrom = 0;

        while (searchFrom < lower.length()) {
            int nameIdx = lower.indexOf(attrLower + "=\"", searchFrom);
            if (nameIdx == -1) {
                nameIdx = lower.indexOf(attrLower + "=", searchFrom);
            }
            if (nameIdx == -1) {
                return null;
            }

            // Verify word boundary: attribute must be preceded by ';', ' ', or be at start
            if (nameIdx > 0) {
                char prev = lower.charAt(nameIdx - 1);
                if (prev != ';' && prev != ' ' && prev != '\t') {
                    searchFrom = nameIdx + attrLower.length();
                    continue;
                }
            }

            int valueStart = headers.indexOf('"', nameIdx);
            if (valueStart == -1) {
                return null;
            }
            int valueEnd = headers.indexOf('"', valueStart + 1);
            if (valueEnd == -1) {
                return null;
            }

            String value = headers.substring(valueStart + 1, valueEnd);
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private static String extractContentType(String headers) {
        String lower = headers.toLowerCase();
        int idx = lower.indexOf("content-type:");
        if (idx == -1) {
            return null;
        }
        int start = idx + 13;
        int end = headers.indexOf('\r', start);
        if (end == -1) {
            end = headers.indexOf('\n', start);
        }
        if (end == -1) {
            end = headers.length();
        }
        return headers.substring(start, end).trim();
    }

    /**
     * Finds the first occurrence of {@code target} in {@code data} starting at {@code from}.
     */
    private static int indexOf(byte[] data, byte[] target, int from) {
        outer:
        for (int i = from; i <= data.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (data[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
