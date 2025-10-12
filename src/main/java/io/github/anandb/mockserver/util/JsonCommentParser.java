package io.github.anandb.mockserver.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Parser for JSON documents with extended syntax support:
 * - C++ style comments: // and /* *\/
 * - Multiline strings using backticks (`)
 */
public class JsonCommentParser {

    private static final JsonMapper objectMapper = MapperSupplier.getMapper();

    /**
     * Parses a JSON string that may contain comments and multiline strings.
     * Comments are removed and multiline strings are converted to standard JSON strings.
     *
     * @param jsonWithComments The JSON string with comments and/or multiline strings
     * @return A JsonNode with comments removed and multiline strings converted
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public static JsonNode parse(String jsonWithComments) {
        if (jsonWithComments == null) {
            throw new IllegalArgumentException("JSON string cannot be null");
        }

        String processedJson = removeCommentsAndConvertMultilineStrings(jsonWithComments);

        try {
            return objectMapper.readTree(processedJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a JSON string to remove comments and convert multiline strings.
     * Returns the cleaned JSON string without attempting to parse it.
     *
     * @param jsonWithComments The JSON string with comments and/or multiline strings
     * @return A cleaned JSON string
     * @throws IllegalArgumentException if the input is null
     */
    public static String clean(String jsonWithComments) {
        if (jsonWithComments == null) {
            throw new IllegalArgumentException("JSON string cannot be null");
        }

        return removeCommentsAndConvertMultilineStrings(jsonWithComments);
    }

    /**
     * Removes comments and converts multiline strings in the JSON text.
     */
    private static String removeCommentsAndConvertMultilineStrings(String json) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        int length = json.length();

        while (i < length) {
            // Check for multiline string (`)
            if (json.charAt(i) == '`') {
                i = processMultilineString(json, i, result);
                continue;
            }

            // Check for single-line comment (//)
            if (i + 1 < length && json.charAt(i) == '/' && json.charAt(i + 1) == '/') {
                i = skipSingleLineComment(json, i);
                continue;
            }

            // Check for multi-line comment (/* */)
            if (i + 1 < length && json.charAt(i) == '/' && json.charAt(i + 1) == '*') {
                i = skipMultiLineComment(json, i);
                continue;
            }

            // Check for regular string (to avoid treating // or /* inside strings as comments)
            if (json.charAt(i) == '"') {
                i = processRegularString(json, i, result);
                continue;
            }

            // Regular character
            result.append(json.charAt(i));
            i++;
        }

        return result.toString();
    }

    /**
     * Processes a multiline string starting with ` and converts it to a regular JSON string.
     */
    private static int processMultilineString(String json, int start, StringBuilder result) {
        int i = start + 1; // Skip opening `
        StringBuilder multilineContent = new StringBuilder();

        // Find the closing `
        while (i < json.length()) {
            if (json.charAt(i) == '`') {
                // Found closing `, convert the content
                String converted = convertMultilineToJsonString(multilineContent.toString());
                result.append('"').append(converted).append('"');
                return i + 1; // Skip closing `
            }

            multilineContent.append(json.charAt(i));
            i++;
        }

        throw new IllegalArgumentException("Unclosed multiline string starting at position " + start);
    }

    /**
     * Converts multiline string content to valid JSON string content.
     * - Newlines are converted to \n
     * - Double quotes are escaped
     * - Backslashes are escaped
     */
    private static String convertMultilineToJsonString(String content) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            switch (c) {
                case '\n' -> result.append("\\n");
                case '\r' -> {
                    // Skip \r or convert to \n if not followed by \n
                    if (i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                        // \r\n -> will be handled by \n case
                    } else {
                        result.append("\\n");
                    }
                }
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\t' -> result.append("\\t");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                default -> result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Processes a regular JSON string (handles escape sequences).
     */
    private static int processRegularString(String json, int start, StringBuilder result) {
        result.append(json.charAt(start)); // Append opening "
        int i = start + 1;

        while (i < json.length()) {
            char c = json.charAt(i);
            result.append(c);

            if (c == '\\') {
                // Escape sequence, skip next character
                i++;
                if (i < json.length()) {
                    result.append(json.charAt(i));
                }
            } else if (c == '"') {
                // End of string
                return i + 1;
            }

            i++;
        }

        throw new IllegalArgumentException("Unclosed string starting at position " + start);
    }

    /**
     * Skips a single-line comment (//).
     */
    private static int skipSingleLineComment(String json, int start) {
        int i = start + 2; // Skip //

        while (i < json.length() && json.charAt(i) != '\n' && json.charAt(i) != '\r') {
            i++;
        }

        // Include the newline character if present
        if (i < json.length() && (json.charAt(i) == '\n' || json.charAt(i) == '\r')) {
            i++;
            // Handle \r\n
            if (i < json.length() && json.charAt(i - 1) == '\r' && json.charAt(i) == '\n') {
                i++;
            }
        }

        return i;
    }

    /**
     * Skips a multi-line comment (/* *\/).
     */
    private static int skipMultiLineComment(String json, int start) {
        int i = start + 2; // Skip /*

        while (i + 1 < json.length()) {
            if (json.charAt(i) == '*' && json.charAt(i + 1) == '/') {
                return i + 2; // Skip */
            }
            i++;
        }

        throw new IllegalArgumentException("Unclosed multi-line comment starting at position " + start);
    }
}
