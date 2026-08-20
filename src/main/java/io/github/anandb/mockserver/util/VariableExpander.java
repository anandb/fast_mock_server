package io.github.anandb.mockserver.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utility class for expanding environment variables in strings.
 * Supports @{VARIABLE} and @{VARIABLE:-DEFAULT_VALUE} syntax.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VariableExpander {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("@\\{([^}:]+)(?::-(.*?))?}");

    /**
     * Expands environment variables in the given text.
     *
     * @param text The text containing variable expressions
     * @return The text with variables expanded
     * @throws IllegalArgumentException if a variable is not found and no default value is provided
     */
    public static String expand(String text) {
        return expand(text, System.getenv());
    }

    /**
     * Expands variables in the given text using the provided map.
     *
     * @param text      The text containing variable expressions
     * @param variables The map of variables to use for expansion
     * @return The text with variables expanded
     * @throws IllegalArgumentException if a variable is not found and no default value is provided
     */
    public static String expand(String text, java.util.Map<String, String> variables) {
        if (text == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        int lastMatchEnd = 0;

        while (matcher.find()) {
            result.append(text, lastMatchEnd, matcher.start());
            String variableName = matcher.group(1);
            String value = variables.getOrDefault(variableName, matcher.group(2));

            if (isBlank(value)) {
                throw new IllegalArgumentException("Variable not found: " + variableName);
            }

            // Escape JSON-special characters to prevent injection when the
            // expanded value is embedded in a JSON string context.
            result.append(escapeForJson(value));
            lastMatchEnd = matcher.end();
        }

        result.append(text.substring(lastMatchEnd));
        return result.toString();
    }

    /**
     * Escapes characters that are special in JSON string values.
     * This prevents environment variable values from breaking out of
     * JSON string delimiters or injecting control structures.
     */
    static String escapeForJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"'  -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
