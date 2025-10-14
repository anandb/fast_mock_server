package io.github.anandb.mockserver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for detecting FreeMarker template syntax in strings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FreemarkerTemplateDetector {

    /**
     * Checks if a string contains FreeMarker template syntax.
     *
     * @param content the string to check
     * @return true if the content contains FreeMarker syntax, false otherwise
     */
    public static boolean isFreemarkerTemplate(String content) {
        return content.contains("${") ||
               content.contains("<#") ||
               content.contains("[#") ||
               content.contains("<@") ||
               content.contains("[@");
    }
}
