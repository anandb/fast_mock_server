package io.github.anandb.mockserver.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class JsonCommentParserDebugTest {

    @Test
    void debugComplexExample() {
        String json = """
            {
                // User configuration
                "user": {
                    "name": "John Doe",
                    /* Contact info
                       Multiple lines */
                    "email": "john@example.com"
                },
                "bio": `John is a software engineer.
            He loves coding and solving problems.
            His motto: "Keep it simple"`
            }
            """;

        log.info("Input JSON:");
        log.info(json);
        log.info("\n--- Processing ---\n");

        try {
            String processed = getProcessedJson(json);
            log.info("Processed JSON:");
            log.info(processed);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }

    private String getProcessedJson(String json) {
        // Access the private method through reflection for debugging
        try {
            java.lang.reflect.Method method = JsonCommentParser.class.getDeclaredMethod(
                "removeCommentsAndConvertMultilineStrings", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
