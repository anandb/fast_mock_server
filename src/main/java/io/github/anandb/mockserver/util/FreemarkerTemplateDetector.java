package io.github.anandb.mockserver.util;

/**
 * Utility class for detecting FreeMarker template syntax in strings.
 */
public class FreemarkerTemplateDetector {
    /**
     * Checks if a string contains FreeMarker template syntax.
     *
     * @param content the string to check
     * @return true if the content contains FreeMarker syntax, false otherwise
     */
    public static boolean isFreemarkerTemplate(String content) {
        if (content == null) {
            return false;
        }
        return content.contains("${") || content.contains("<#") || content.contains("[#") || content.contains("<@") || content.contains("[@");
    }

    private FreemarkerTemplateDetector() {
    }
}
